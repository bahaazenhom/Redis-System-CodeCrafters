package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class BLPOPCommand implements CommandStrategy {
    private final DataStore dataStore;

    public BLPOPCommand(DataStore dataStore) {
        this.dataStore = dataStore;
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
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            String listKey = arguments.get(0);
            double timestamp = Double.parseDouble(arguments.get(1));

            String value = dataStore.BLPOP(listKey, timestamp);

            List<String> result = new ArrayList<>();
            result.add(listKey);
            result.add(value);

            if (value == null)
                clientOutput.write(RESPSerializer.nullArray());
            else
                clientOutput.write(RESPSerializer.array(result));

            clientOutput.flush();
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

}
