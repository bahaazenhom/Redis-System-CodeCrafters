package replication;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import server.ClientHandler;
import server.CommandPropagationHandler;
import server.ServerInstance;
import server.SlaveAckHandler;

public class ReplicationManager {
    private MasterNode masterNode;// reference to the master node if this instance is a master
    private SlaveNode slaveNode;// reference to the slave node if this instance is a slave
    private ConcurrentHashMap<Integer, ClientConnection> slaveNodesSockets;// map of slave port to their connections if
                                                                           // this instance is a master
    private static ReplicationManager replicationManager = null;// singleton instance of ReplicationManager
    private static boolean isSlaveNode = false;

    private ReplicationManager() {
        this.masterNode = null;
        this.slaveNodesSockets = new ConcurrentHashMap<>();
    }

    public static ReplicationManager create() {
        if (replicationManager != null) {
            return replicationManager;
        }
        replicationManager = new ReplicationManager();
        return replicationManager;
    }

    public ServerInstance createReplica(int port, CommandExecuter commandExecuter, String[] args) throws IOException {
        if (args[0].equals("master")) {
            MasterNode master = new MasterNode("localhost", port, commandExecuter, "master");
            this.masterNode = master;
            return master;
        } else {
            String masterHost = args[1];
            int masterPort = Integer.parseInt(args[2]);
            System.out.println("-------------------------------- " + masterHost + ":" + masterPort);
            this.slaveNode = new SlaveNode("localhost", port, commandExecuter, "slave", masterHost, masterPort);
            isSlaveNode = true;
            masterHandshake(port, masterHost, masterPort);
            return this.slaveNode;
        }
    }

    private void masterHandshake(int slavePort, String masterHost, int masterPort) {
        try {
            Socket socket = new Socket(masterHost, masterPort);
            InputStream input = socket.getInputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // helper lambda to read a CRLF-terminated line from InputStream
            java.util.function.Function<InputStream, String> readLine = (is) -> {
                try {
                    ByteArrayOutputStream lineBuf = new ByteArrayOutputStream();
                    int prev = -1;
                    int curr;
                    while ((curr = is.read()) != -1) {
                        if (prev == '\r' && curr == '\n') {
                            // remove trailing CR from buffer
                            byte[] bytes = lineBuf.toByteArray();
                            if (bytes.length > 0) {
                                return new String(bytes, 0, bytes.length - 1);
                            } else {
                                return "";
                            }
                        }
                        lineBuf.write(curr);
                        prev = curr;
                    }
                } catch (IOException e) {
                    // fall through
                }
                return null;
            };

            String response;

            // ========== 1. PING ==========
            List<String> pingHandShake = new ArrayList<>();
            pingHandShake.add("PING");
            out.write(RESPSerializer.array(pingHandShake));
            out.flush();
            response = readLine.apply(input);
            System.out.println("ping response: " + response);

            // ========== 2. REPLCONF listening-port ==========
            List<String> listeningPortHandShake = new ArrayList<>();
            listeningPortHandShake.add("REPLCONF");
            listeningPortHandShake.add("listening-port");
            listeningPortHandShake.add(String.valueOf(slavePort));
            out.write(RESPSerializer.array(listeningPortHandShake));
            out.flush();
            response = readLine.apply(input);
            System.out.println("listening-port response: " + response);

            // ========== 3. REPLCONF capa psync2 ==========
            List<String> capaHandShake = new ArrayList<>();
            capaHandShake.add("REPLCONF");
            capaHandShake.add("capa");
            capaHandShake.add("psync2");
            out.write(RESPSerializer.array(capaHandShake));
            out.flush();
            response = readLine.apply(input);
            System.out.println("capa response: " + response);

            // ========== 4. PSYNC ==========
            List<String> psyncHandShake = new ArrayList<>();
            psyncHandShake.add("PSYNC");
            psyncHandShake.add("?");
            psyncHandShake.add("-1");
            out.write(RESPSerializer.array(psyncHandShake));
            out.flush();
            String psyncResponse = readLine.apply(input);
            System.out.println("psync response: " + psyncResponse);

            // ========== 5. Read RDB header (CRLF-terminated) ==========
            String header = readLine.apply(input);
            System.out.println("file header response: " + header);

            if (header == null || !header.startsWith("$")) {
                throw new IOException("Invalid RDB header received: " + header);
            }

            // Parse the byte length from header (e.g. "$88" → 88)
            int rdbLength = Integer.parseInt(header.substring(1));

            // ========== 6. Read binary RDB data into a streaming OutputStream ==========
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(rdbLength, 65536));
            byte[] buffer = new byte[4096];
            int remaining = rdbLength;
            while (remaining > 0) {
                int toRead = Math.min(buffer.length, remaining);
                int bytesRead = input.read(buffer, 0, toRead);
                if (bytesRead == -1)
                    break; // stream closed unexpectedly
                baos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }

            // byte[] fileData = baos.toByteArray();

            // ========== 7. Start replication stream ==========
            // Keep the socket reference in the slave node so it won't be closed
            if (this.slaveNode != null) {
                this.slaveNode.setMasterSocket(socket);
            }

            ClientConnection connection = new ClientConnection(socket.getOutputStream(), socket.getInputStream());
            // Wrap the same InputStream in a BufferedReader for command parsing
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            handleReplicationStreamFromMaster(connection, reader);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReplicationStreamFromMaster(ClientConnection connection, BufferedReader in) {
        // Start a new thread to handle incoming commands from the master
        Thread replicaHandler = new Thread(new CommandPropagationHandler(slaveNode.getCommandExecuter(),
                connection, in));
        replicaHandler.start();
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public ConcurrentHashMap<Integer, ClientConnection> getSlaveNodesSockets() {
        return this.slaveNodesSockets;
    }

    public String getReplicaInfo() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

        // Check if this is the master
        if (masterNode != null && masterNode.getPort() == port) {
            return masterNode.getInfo();
        }

        // Check if this is a slave
        if (slaveNode != null && slaveNode.getPort() == port) {
            return slaveNode.getInfo();
        }

        // Fallback - should not happen
        return "role:unknown";
    }

    public String getCurrentSlaveInfo() {
        SlaveNode slave = slaveNode;
        if (slave != null) {
            return slave.getInfo();
        }

        // Fallback - should not happen
        return "role:unknown";
    }

    public void replicateToSlaves(List<String> command) throws IOException {
        int currentPort = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);
        // Only the master should replicate commands
        if (masterNode == null || masterNode.getPort() != currentPort) {
            return;
        }

        for (ClientConnection slaveConnection : slaveNodesSockets.values()) {
            slaveConnection.write(RESPSerializer.array(command));
            slaveConnection.flush();
        }
    }

    public SlaveNode getSlaveNode() {
        return this.slaveNode;
    }

    // You are the aster here
    public void askForOffsetAcksFromSlaves() {
        for (ClientConnection slaveMasterConnection : slaveNodesSockets.values()) {
            try {
                List<String> ackCommand = new ArrayList<>();
                ackCommand.add("REPLCONF");
                ackCommand.add("GETACK");
                ackCommand.add("*");
                slaveMasterConnection.write(RESPSerializer.array(ackCommand));
                slaveMasterConnection.flush();
                System.out.println("Master asked slave for ACK offset "+ackCommand);
                // Wait for the acknowledgment from the slave
                handleAcksCommandsReceivedFromSlaves(slaveMasterConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAcksCommandsReceivedFromSlaves(ClientConnection connection) {
        // Start a new thread to handle incoming acks commands from slaves
        new SlaveAckHandler(connection, masterNode.getCommandExecuter()).run();
    }

    // You are the Slave here
    public void responseToMasterWithAckOffset(ClientConnection slaveMasterConnection) {
        SlaveNode slave = this.slaveNode;
        if (slave != null) {
            try {
                List<String> ackCommand = new ArrayList<>();
                ackCommand.add("REPLCONF");
                ackCommand.add("ACK");
                ackCommand.add(String.valueOf(slave.getReplicationOffset()));
                slaveMasterConnection.write(RESPSerializer.array(ackCommand));
                slaveMasterConnection.flush();

            } catch (Exception e) {
                e.printStackTrace();// Log the error
            }
        }
    }

    public static boolean isSlaveNode() {
        return isSlaveNode;
    }

    public void updateSlaveOffset(long length) {
        if (this.slaveNode != null) {
            this.slaveNode.incrementReplicationOffset(length);
        }
    }

    public void updateMasterOffset(long length) {
        if (this.masterNode != null) {
            this.masterNode.incrementReplicationOffset(length);
        }
    }
}
