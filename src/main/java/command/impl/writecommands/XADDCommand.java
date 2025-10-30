package command.impl.writecommands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.DataStore;
import storage.exception.InvalidStreamEntryException;

public class XADDCommand implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public XADDCommand(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 4) {
            throw new IllegalArgumentException("Wrong number of arguments of the 'XADD' command.");
        }
        if ((arguments.size() - 2) % 2 != 0) {
            throw new IllegalArgumentException("wrong number of arguments for XADD");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String streamKey = arguments.get(0);
            String entryID = arguments.get(1);
            HashMap<String, String> entryValues = new HashMap<>();
            for (int index = 2; index < arguments.size(); index += 2) {
                String key = arguments.get(index);
                String value = arguments.get(index + 1);
                entryValues.put(key, value);
            }

            entryID = dataStore.xadd(streamKey, entryID, entryValues);

            // If this node is a replica, do not send replies back to the master/client
            if (ReplicationManager.isSlaveNode())
                return;

            clientOutput.write(RESPSerializer.bulkString(entryID));
            clientOutput.flush();

            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("XADD");
            commandForReplication.addAll(arguments);

            // Update master offset
            updateMasterOffset(RESPSerializer.array(commandForReplication).getBytes().length);
            // Replication to replicas
            replicateToReplicas(commandForReplication);
        } catch (InvalidStreamEntryException e) {
            try {
                clientOutput.write(RESPSerializer.error(e.getMessage()));
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
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
