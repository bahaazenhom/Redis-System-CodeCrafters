package replication;

public class ReplicationManager {

    private String masterAddress;
    private int masterPort;

    public ReplicationManager(String masterAddress, int masterPort) {
        this.masterAddress = masterAddress;
        this.masterPort = masterPort;
    }

    public void startReplication() {
        // Logic to start replication from master
    }

    public void stopReplication() {
        // Logic to stop replication
    }

}
