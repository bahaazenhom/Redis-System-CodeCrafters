package command.handlers.sortedset;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class ZREMHandler implements CommandStrategy {
    private final DataStore dataStore;

    public ZREMHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String memberName = arguments.get(1);

            int removedCount = dataStore.zrem(key, memberName);
            clientOutput.write(RESPSerializer.integer(removedCount));
            clientOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("ZREM command requires exactly 2 arguments.");
        }
    }

}
