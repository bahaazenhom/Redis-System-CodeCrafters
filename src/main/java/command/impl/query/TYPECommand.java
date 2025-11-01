package command.impl.query;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import storage.DataStore;

public class TYPECommand implements CommandStrategy {
    private final DataStore dataStore;

    public TYPECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'TYPE' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            if (!dataStore.exists(key))
                clientOutput.write(RESPSerializer.bulkString("none"));
            else {
                String type = dataStore.getType(key).toString().toLowerCase();
                clientOutput.write(RESPSerializer.bulkString(type));
            }
            clientOutput.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
