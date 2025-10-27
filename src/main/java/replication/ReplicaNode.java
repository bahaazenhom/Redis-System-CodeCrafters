package replication;

public class ReplicaNode {
    private String id;
    private String port;
    private MasterNode masterNode;

    public ReplicaNode(String id, String port, MasterNode masterNode) {
        this.id = id;
        this.port = port;
        this.masterNode = masterNode;
    }

    public String getId() {
        return id;
    }

    public String getPort() {
        return port;
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }
}
