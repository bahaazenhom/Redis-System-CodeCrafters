package replication;

import java.io.IOException;
import java.net.Socket;
import command.CommandExecuter;
import server.ServerInstance;

public class SlaveNode extends ServerInstance {
    private final int id;

    private final String masterHost;
    private String role = "slave";
    private final String replicationId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private long replicationOffset = 0L;

    public SlaveNode(String host, int port, CommandExecuter commandExecuter, String role, String masterHost,
            int masterPort) throws IOException {
        super(host, port, commandExecuter, role);
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.id = port; // For simplicity, using port as ID
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

    public int getId() {
        return id;
    }

    public String getInfo() {
        String info = "role:" + this.role + "\r\n"
                + "master_replid:" + replicationId + "\r\n"
                + "master_repl_offset:" + replicationOffset;

        return info;
    }

    public String getMasterHost() {
        return masterHost;
    }

    private final int masterPort;
    private Socket masterSocket;

    public void setMasterSocket(Socket masterSocket) {
        this.masterSocket = masterSocket;
    }

    public Socket getMasterSocket() {
        return masterSocket;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public void incrementReplicationOffset(long length) {
        this.replicationOffset += length;
    }

}
