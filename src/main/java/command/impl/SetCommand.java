package command.impl;

import command.CommandStrategy;
import command.ResponseWriter.ResponseWriter;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.core.RedisValue;
import storage.types.StringValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SetCommand implements CommandStrategy {
    private final DataStore dataStore;

    public SetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
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
    public void execute(List<String> arguments, ResponseWriter clientOutput) {
        try {
            String key = arguments.get(0);
            String value = arguments.get(1);

            Long expiryTimeStamp = parseExpiryOptions(arguments);
            RedisValue redisValue = new StringValue(value, expiryTimeStamp);
            dataStore.setValue(key, redisValue);
            clientOutput.writeText(RESPSerializer.simpleString("OK"));
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

}
