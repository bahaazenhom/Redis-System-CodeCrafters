package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ResponseWriter;
import protocol.RESPSerializer;
import storage.DataStore;

public class LPOPCommand implements CommandStrategy {
    private final DataStore dataStore;

    public LPOPCommand(DataStore dataStore) {
        this.dataStore = dataStore;
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
    public void execute(List<String> arguments, ResponseWriter clientOutput) {
        try {
            String listName = arguments.get(0);
            Long counter = arguments.size() > 1 ? Long.parseLong(arguments.get(1)) : null;
            if (!dataStore.exists(listName)) {
                clientOutput.write(RESPSerializer.nullBulkString());
                clientOutput.flush();
                return;
            }
            List<String> firstValues = dataStore.lpop(listName, counter);
            if (counter == null) {
                clientOutput.write(RESPSerializer.bulkString(firstValues.get(0)));
            } else {
                clientOutput.write(RESPSerializer.array(firstValues));
            }
            clientOutput.flush();

        } catch (IOException exception) {
            throw new RuntimeException();
        }
    }

}
