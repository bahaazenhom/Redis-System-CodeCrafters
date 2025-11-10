package command.handlers.sortedset;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class ZSCOREHandler implements CommandStrategy {
    private final DataStore dataStore;

    public ZSCOREHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String memberName = arguments.get(1);
            double score = dataStore.zscore(key, memberName);
            
            if (score == -1)
                clientOutput.write(RESPSerializer.nullBulkString());
            else
                clientOutput.write(RESPSerializer.bulkString(String.valueOf(score)));

            clientOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("ZSCORE command requires exactly 2 arguments.");
        }
    }

}
