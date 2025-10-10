package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SetCommand implements CommandStrategy {
    private final DataStore dataStore;

    public SetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() < 2) {
                clientOutput.write(RESPSerializer.error("wrong number of arguments for 'set' command"));
                clientOutput.flush();
                return;
            }

            String key = arguments.get(0);
            String value = arguments.get(1);

            Long expiryTimeStamp = parseExpiryOptions(arguments);
            if (expiryTimeStamp != null) {
                dataStore.setWithExpiry(key, value, expiryTimeStamp);
            } else {
                dataStore.set(key, value);
            }

            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Long parseExpiryOptions(List<String> arguments) {
        if (arguments.size() < 4)
            return null;

        String option = arguments.get(2).toUpperCase();

        try {
            long timeValue = Long.parseLong(arguments.get(3));

            switch (option) {
                case "EX":
                    return System.currentTimeMillis() + (timeValue * 1000);
                case "PX":
                    return System.currentTimeMillis() + timeValue;

                default:
                    throw new IllegalArgumentException("Invalid expiry option: " + option + " in the 'set' command");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time value for expiry option in the 'set' command");
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Syntax error in the 'set' command");
        }
    }

}
