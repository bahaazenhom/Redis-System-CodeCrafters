package command.impl.writecommands;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.DataStore;
import storage.types.ListValue;

public class LPUSHCommand implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public LPUSHCommand(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LPUSH' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String listName = arguments.get(0);
            List<String> values = arguments.subList(1, arguments.size());
            if (!dataStore.exists(listName))
                dataStore.setValue(listName, new ListValue(new ArrayDeque<>()));
            long size = dataStore.lpush(listName, values);

            // If this node is a replica, do not send replies or replicate
            if (isSlaveNode()) return;

            clientOutput.write(RESPSerializer.integer(size));
            clientOutput.flush();

            // Replication to replicas (only on master)
            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("LPUSH");
            commandForReplication.addAll(arguments);
            replicateToReplicas(commandForReplication);

        } catch (IOException e) {
            throw new RuntimeException(e);
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
    public boolean isSlaveNode() {
        return replicationManager.isSlaveNode();
    }

}
