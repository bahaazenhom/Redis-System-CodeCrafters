package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class INCRCommand implements CommandStrategy {
    private final DataStore dataStore;

    public INCRCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            String key = arguments.get(0);
            long newValue = dataStore.incr(key);
            clientOutput.write(RESPSerializer.integer(newValue));
            clientOutput.flush();
        } catch (NumberFormatException nfe) {
            try {
                clientOutput.write(RESPSerializer.error("value is not an integer or out of range"));
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("INCR command requires exactly one argument.");
        }
    }

}
