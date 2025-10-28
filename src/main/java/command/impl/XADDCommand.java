package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ResponseWriter;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.exception.InvalidStreamEntryException;

public class XADDCommand implements CommandStrategy {
    private final DataStore dataStore;

    public XADDCommand(DataStore dataStore) {
        this.dataStore = dataStore;
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
    public void execute(List<String> arguments, ResponseWriter clientOutput) {
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
            clientOutput.writeText(RESPSerializer.bulkString(entryID));
        } catch (InvalidStreamEntryException e) {
            try {
                clientOutput.writeText(RESPSerializer.error(e.getMessage()));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
