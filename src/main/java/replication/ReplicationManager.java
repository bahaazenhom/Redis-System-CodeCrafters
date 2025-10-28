package replication;

import java.io.IOException;
import java.util.HashMap;

import command.CommandExecuter;
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
            MasterNode master = new MasterNode("localhost", port, commandExecuter, args[0]);
            this.masterNode = master;
            return master;
        } else {
            String masterHost = args[1];
            int masterPort = Integer.parseInt(args[2]);
            MasterNode masterNode = new MasterNode(masterHost, masterPort, commandExecuter, "master");
            this.masterNode = masterNode;
            SlaveNode slave = new SlaveNode("local host", port, commandExecuter, args[0], this.masterNode);
            this.slaveNodes.put(port, slave);
            return slave;
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
}
