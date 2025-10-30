package command.impl.writecommands;

import java.util.List;

public interface Replicable {
    void replicateToReplicas(List<String> command);
    void updateMasterOffset(long offset);
}
