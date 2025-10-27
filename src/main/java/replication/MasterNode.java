package replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandExecuter;
import server.ServerInstance;

public class MasterNode extends ServerInstance {
    private final String role = "master";
    private final List<SlaveNode> connectedSlaves = new ArrayList<>();
    private final String replicationId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private long replicationOffset = 0L;

    public MasterNode(int port, CommandExecuter commandExecuter, String serverRole) throws IOException {
        super(port, commandExecuter, serverRole);
        System.out.println("Initializing Master Node on port " + port);
    }

    public void addSlave(SlaveNode slave) {
        connectedSlaves.add(slave);
    }

    public String getRole() {
        return role;
    }

    public List<SlaveNode> getConnectedSlaves() {
        return connectedSlaves;
    }

    public String getReplicationId() {
        return replicationId;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    public String getInfo() {
        String info = "role:" + role + "\r\n"
                + "master_replid:" + replicationId + "\r\n"
                + "master_repl_offset:" + replicationOffset;

        return info;
    }
}