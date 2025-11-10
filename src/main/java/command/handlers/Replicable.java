package command.handlers;

import java.util.List;

public interface Replicable {
    void replicateToReplicas(List<String> command);
    void updateMasterOffset(long offset);
}
