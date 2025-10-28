package replication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import command.CommandExecuter;
import protocol.RESPSerializer;
import server.ServerInstance;

public class ReplicationManager {
    private MasterNode masterNode;
    private HashMap<Integer, SlaveNode> slaveNodes;
    private static ReplicationManager replicationManager = null;

    private ReplicationManager() {
        this.masterNode = null;
        this.slaveNodes = new HashMap<>();
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
            SlaveNode slave = new SlaveNode("local host", port, commandExecuter, "slave", masterHost, masterPort);
            this.slaveNodes.put(port, slave);
            masterHandshake(port, masterHost, masterPort);
            return slave;
        }
    }

    private void masterHandshake(int slavePort, String masterHost, int masterPort) {
        try {
            Socket socket = new Socket(masterHost, masterPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String response;

            List<String> pingHandShake = new ArrayList<>();
            pingHandShake.add("PING");
            out.write(RESPSerializer.array(pingHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("ping response: " + response);

            List<String> listeningPortHandShake = new ArrayList<>();
            listeningPortHandShake.add("REPLCONF");
            listeningPortHandShake.add("listening-port");
            listeningPortHandShake.add(String.valueOf(slavePort));
            out.write(RESPSerializer.array(listeningPortHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("listening-port response: " + response);

            List<String> capaHandShake = new ArrayList<>();
            capaHandShake.add("REPLCONF");
            capaHandShake.add("capa");
            capaHandShake.add("psync2");
            out.write(RESPSerializer.array(capaHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("capa response: " + response);

            List<String> psyncHandShake = new ArrayList<>();
            psyncHandShake.add("PSYNC");
            psyncHandShake.add("?");
            psyncHandShake.add("-1");
            out.write(RESPSerializer.array(psyncHandShake));
            out.flush();
            response = in.readLine();
            System.out.println("psync response: " + response);


            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public HashMap<Integer, SlaveNode> getSlaveNodes() {
        return this.slaveNodes;
    }

    public String getServerRole() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

        // Check if this is the master
        if (masterNode != null && masterNode.getPort() == port) {
            return "master";
        }

        // Check if this is a slave
        if (slaveNodes.containsKey(port)) {
            return "slave";
        }

        // Fallback
        return "unknown";
    }

    public String getReplicaInfo() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

        // Check if this is the master
        if (masterNode != null && masterNode.getPort() == port) {
            return masterNode.getInfo();
        }

        // Check if this is a slave
        SlaveNode slave = slaveNodes.get(port);
        if (slave != null) {
            return slave.getInfo();
        }

        // Fallback - should not happen
        return "role:unknown";
    }

    public String getCurrentSlaveInfo() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

        // Check if this is a slave
        SlaveNode slave = slaveNodes.get(port);
        if (slave != null) {
            return slave.getInfo();
        }

        // Fallback - should not happen
        return "role:unknown";
    }
}
