package replication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import server.ClientHandler;
import server.ReplicaHandler;
import server.ServerInstance;

public class ReplicationManager {
    private MasterNode masterNode;
    private SlaveNode slaveNode;
    private List<ClientConnection> slaveNodesSockets;
    private static ReplicationManager replicationManager = null;

    private ReplicationManager() {
        this.masterNode = null;
        this.slaveNodesSockets = new ArrayList<>();
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
            this.slaveNode = new SlaveNode("local host", port, commandExecuter, "slave", masterHost, masterPort);
            masterHandshake(port, masterHost, masterPort);
            return this.slaveNode;
        }
    }

    private void masterHandshake(int slavePort, String masterHost, int masterPort) {
        try {
            Socket socket = new Socket(masterHost, masterPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String response;

            // PING
            List<String> pingHandShake = new ArrayList<>();
            pingHandShake.add("PING");
            out.write(RESPSerializer.array(pingHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("ping response: " + response);

            // REPLCONF listening-port
            List<String> listeningPortHandShake = new ArrayList<>();
            listeningPortHandShake.add("REPLCONF");
            listeningPortHandShake.add("listening-port");
            listeningPortHandShake.add(String.valueOf(slavePort));
            out.write(RESPSerializer.array(listeningPortHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("listening-port response: " + response);

            // REPLCONF capa psync2
            List<String> capaHandShake = new ArrayList<>();
            capaHandShake.add("REPLCONF");
            capaHandShake.add("capa");
            capaHandShake.add("psync2");
            out.write(RESPSerializer.array(capaHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("capa response: " + response);

            // PSYNC ? -1 <slave-port>
            List<String> psyncHandShake = new ArrayList<>();
            psyncHandShake.add("PSYNC");
            psyncHandShake.add("?");
            psyncHandShake.add("-1");
            out.write(RESPSerializer.array(psyncHandShake));
            out.flush();
            String psyncResponse = in.readLine();
            System.out.println("psync response: " + psyncResponse);

            String header = in.readLine();
            System.out.println("file header response: " + header);
            String fileData = in.readLine();
            System.out.println("RDB file data response: " + fileData);

            // Start handling replication stream
            handleReplicationStream(socket.getOutputStream(), in);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReplicationStream(OutputStream outputStream, BufferedReader in) {
        // Start a new thread to handle incoming commands from the master
        Thread replicaHandler = new Thread(new ReplicaHandler(slaveNode.getCommandExecuter(),
                new ClientConnection(outputStream), in));
        replicaHandler.start();
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public List<ClientConnection> getSlaveNodesSockets() {
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

        for (ClientConnection slaveConnection : slaveNodesSockets) {
            slaveConnection.write(RESPSerializer.array(command));
            slaveConnection.flush();
        }
    }

    public SlaveNode getSlaveNode() {
        return this.slaveNode;
    }
}
