package command.impl.writecommands;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.DataStore;
import storage.core.RedisValue;
import storage.types.StringValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SetCommand implements CommandStrategy, Replicable {
    private final DataStore dataStore;
    private final ReplicationManager replicationManager;

    public SetCommand(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'set' command");
        }
        // Validate expiry options if present
        if (arguments.size() >= 4) {
            String option = arguments.get(2).toUpperCase();
            if (!option.equals("EX") && !option.equals("PX")) {
                throw new IllegalArgumentException("Invalid expiry option: " + option + " in the 'set' command");
            }
            try {
                Long.parseLong(arguments.get(3));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time value for expiry option in the 'set' command");
            }
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String value = arguments.get(1);

            Long expiryTimeStamp = parseExpiryOptions(arguments);
            RedisValue redisValue = new StringValue(value, expiryTimeStamp);
            dataStore.setValue(key, redisValue);
            // If this node is a replica, do not send replies or replicate
            if (ReplicationManager.isSlaveNode())
                return;

            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();

            List<String> commandForReplication = new ArrayList<>();
            commandForReplication.add("SET");
            commandForReplication.addAll(arguments);

            // Update master offset
            updateMasterOffset(RESPSerializer.array(commandForReplication).getBytes().length);
            // Replication to replicas
            replicateToReplicas(commandForReplication);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Long parseExpiryOptions(List<String> arguments) {
        if (arguments.size() < 4)
            return null;

        String option = arguments.get(2).toUpperCase();
        long timeValue = Long.parseLong(arguments.get(3));

        switch (option) {
            case "EX":
                return System.currentTimeMillis() + (timeValue * 1000);
            case "PX":
                return System.currentTimeMillis() + timeValue;
            default:
                return null;
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
