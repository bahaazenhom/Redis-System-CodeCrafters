package command.impl.writecommands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.DataStore;

public class LPOPCommand implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public LPOPCommand(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LPOP' command");
        }
        if (arguments.size() > 1) {
            try {
                Long.parseLong(arguments.get(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("value is not an integer or out of range");
            }
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String listName = arguments.get(0);
            Long counter = arguments.size() > 1 ? Long.parseLong(arguments.get(1)) : null;
            if (!dataStore.exists(listName)) {
                clientOutput.write(RESPSerializer.nullBulkString());
                clientOutput.flush();
                return;
            }
            List<String> firstValues = dataStore.lpop(listName, counter);

            // If this node is a replica, do not send replies or replicate
            if (ReplicationManager.isSlaveNode()) return;

            if (counter == null) {
                clientOutput.write(RESPSerializer.bulkString(firstValues.get(0)));
            } else {
                clientOutput.write(RESPSerializer.array(firstValues));
            }
            clientOutput.flush();

            // Replication to replicas
            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("LPOP");
            commandForReplication.addAll(arguments);
            replicateToReplicas(commandForReplication);

        } catch (IOException exception) {
            throw new RuntimeException();
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

}
