package command.handlers.string;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class TYPEHandler implements CommandStrategy {
    private final DataStore dataStore;

    public TYPEHandler(DataStore dataStore) {
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
                clientOutput.write(RESPSerializer.simpleString("none"));
            else {
                String type = dataStore.getType(key).toString().toLowerCase();
                clientOutput.write(RESPSerializer.simpleString(type));
            }
            clientOutput.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
