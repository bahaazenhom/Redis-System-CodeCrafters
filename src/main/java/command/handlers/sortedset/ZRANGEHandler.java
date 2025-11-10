package command.handlers.sortedset;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class ZRANGEHandler implements CommandStrategy {
    private final DataStore dataStore;

    public ZRANGEHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            int start = Integer.parseInt(arguments.get(1));
            int end = Integer.parseInt(arguments.get(2));

            List<String> rangeMembers = dataStore.zrange(key, start, end);

            clientOutput.write(RESPSerializer.array(rangeMembers));
            clientOutput.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 3) {
            throw new IllegalArgumentException("Wrong number of arguments for 'ZRANGE' command");
        }
    }

}
