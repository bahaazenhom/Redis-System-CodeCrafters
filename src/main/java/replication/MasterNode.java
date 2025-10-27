package replication;

import java.util.ArrayList;
import java.util.List;

public class MasterNode {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String port;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    private List<ReplicaNode> replicas;

    public MasterNode(String id, String port) {
        this.id = id;
        this.port = port;
        this.replicas = new ArrayList<>();
    }

    public void addReplica(ReplicaNode replica) {
        replicas.add(replica);
    }

    public void removeReplica(ReplicaNode replica) {
        replicas.remove(replica);
    }

    public List<ReplicaNode> getReplicas() {
        return replicas;
    }
}
