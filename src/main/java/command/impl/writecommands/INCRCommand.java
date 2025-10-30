package command.impl.writecommands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.DataStore;

public class INCRCommand implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public INCRCommand(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            long newValue = dataStore.incr(key);

            // If this node is a replica, do not send replies or replicate
            if (ReplicationManager.isSlaveNode())
                return;

            clientOutput.write(RESPSerializer.integer(newValue));
            clientOutput.flush();

            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("INCR");
            commandForReplication.addAll(arguments);

            // Update master offset
            updateMasterOffset(RESPSerializer.array(commandForReplication).getBytes().length);
            // Replication to replicas
            replicateToReplicas(commandForReplication);

        } catch (NumberFormatException nfe) {
            try {
                clientOutput.write(RESPSerializer.error(nfe.getMessage()));
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("INCR command requires exactly one argument.");
        }
    }

    @Override
    public void replicateToReplicas(List<String> command) {
        try {
            replicationManager.replicateToSlaves(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMasterOffset(long offset) {
        replicationManager.updateMasterOffset(offset);
    }

}
