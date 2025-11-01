package replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandExecuter;
import server.core.ServerInstance;

public class MasterNode extends ServerInstance {
    private final String role = "master";
    private final List<SlaveNode> connectedSlaves = new ArrayList<>();
    private final String id = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private long offset = 0L;

    public MasterNode(String host, int port, CommandExecuter commandExecuter, String serverRole) throws IOException {
        super(host, port, commandExecuter, serverRole);
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

    public String getId() {
        return id;
    }

    public long getOffset() {
        return offset;
    }

    public String getInfo() {
        String info = "role:" + role + "\r\n"
                + "master_replid:" + id + "\r\n"
                + "master_repl_offset:" + offset;

        return info;
    }

    public void incrementReplicationOffset(long length) {
        this.offset += length;
    }
}
