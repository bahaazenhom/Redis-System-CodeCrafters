package command.handlers.list;

import command.handlers.Replicable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import command.CommandStrategy;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import storage.DataStore;
import domain.values.ListValue;

public class RPUSHHandler implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public RPUSHHandler(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("wrong number of arguments for 'RPUSH' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String listName = arguments.get(0);
            List<String> values = arguments.subList(1, arguments.size());

            if (!dataStore.exists(listName))
                dataStore.setValue(listName, new ListValue(new ArrayDeque<>()));
            if (!(dataStore.getValue(listName) instanceof ListValue)) {
                clientOutput.write(
                        RESPSerializer.error("WRONGTYPE Operation against a key holding the wrong kind of value"));
                clientOutput.flush();
                return;
            }
            long size = dataStore.rpush(listName, values);

            // If this node is a replica, do not send replies or replicate
            if (ReplicationManager.isSlaveNode())
                return;

            clientOutput.write(RESPSerializer.integer(size));
            clientOutput.flush();

            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("RPUSH");
            commandForReplication.addAll(arguments);

            // Update master offset
            updateMasterOffset(RESPSerializer.array(commandForReplication).getBytes().length);
            // Replication to replicas
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
    public void updateMasterOffset(long offset) {
        replicationManager.updateMasterOffset(offset);
    }

}
