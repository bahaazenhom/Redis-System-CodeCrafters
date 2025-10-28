package replication;

import java.io.IOException;

import command.CommandExecuter;
import server.ServerInstance;

public class SlaveNode extends ServerInstance {
    private MasterNode masterNode;
    private String role = "slave";
    private final String replicationId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private long replicationOffset = 0L;

    public SlaveNode(String host, int port, CommandExecuter commandExecuter, String role, MasterNode masterNode) throws IOException {
        super(host, port, commandExecuter, role);
        this.masterNode = masterNode;
    }

    public String getRole() {
        return role;
    }

    public String getReplicationId() {
        return replicationId;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public String getInfo() {
        String info = "role:" + role + "\r\n"
                + "master_replid:" + replicationId + "\r\n"
                + "master_repl_offset:" + replicationOffset;

        return info;
    }
}
