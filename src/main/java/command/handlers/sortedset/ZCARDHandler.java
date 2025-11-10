package command.handlers.sortedset;

import java.io.IOException;
import java.util.List;

import javax.xml.crypto.Data;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class ZCARDHandler implements CommandStrategy {
    private final DataStore dataStore;

    public ZCARDHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            int setSize = dataStore.zcard(key);
            clientOutput.write(RESPSerializer.integer(setSize));
            clientOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("ZCARD command requires exactly 1 argument.");
        }
    }

}
