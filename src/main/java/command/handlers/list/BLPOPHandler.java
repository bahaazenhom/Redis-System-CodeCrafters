package command.handlers.list;

import command.handlers.Replicable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import storage.DataStore;

public class BLPOPHandler implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public BLPOPHandler(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'BLPOP' command");
        }
        try {
            double timestamp = Double.parseDouble(arguments.get(1));
            if (timestamp < 0) {
                throw new IllegalArgumentException("timeout is negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeout is not a number or out of range");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String listKey = arguments.get(0);
            double timestamp = Double.parseDouble(arguments.get(1));

            String value = dataStore.BLPOP(listKey, timestamp);

            List<String> result = new ArrayList<>();
            result.add(listKey);
            result.add(value);

            
            
            // Send response to client
            // if this is a slave node, do not send response
            if (ReplicationManager.isSlaveNode()) return;
            
            if (value == null)
            clientOutput.write(RESPSerializer.nullArray());
            else
            clientOutput.write(RESPSerializer.array(result));
            
            clientOutput.flush();
            
            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("BLPOP");
            commandForReplication.addAll(arguments);

            // Update master offset
            updateMasterOffset(RESPSerializer.array(commandForReplication).getBytes().length);
            // Replication to replicas
            replicateToReplicas(commandForReplication);
            
        } catch (InterruptedException exception) {
            try {
                clientOutput.write(RESPSerializer.error("Operation Interrupted"));
                clientOutput.flush();
                return;
            } catch (IOException exception2) {
                throw new RuntimeException(exception2);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
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
