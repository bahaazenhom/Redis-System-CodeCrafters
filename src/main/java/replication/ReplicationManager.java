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

    public ServerInstance createReplica(int port, CommandExecuter commandExecuter, String role) throws IOException {
        if (role.equals("master")) {
            MasterNode master = new MasterNode(port, commandExecuter, role);
            this.masterNode = master;
            return master;
        } else
            return new SlaveNode(port, commandExecuter, role, this.masterNode);
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public HashMap<Integer, SlaveNode> getSlaveNodes() {
        return this.slaveNodes;
    }

    public String getServerRole() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);
        if (port == masterNode.getPort()) {
            return "master";
        }
        return "slave";
    }

    public String getReplicaInfo() {
        int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

        if (masterNode.getPort() == port)
            return masterNode.getInfo();
        else {
            return slaveNodes.get(port).getInfo();
        }
    }
}
