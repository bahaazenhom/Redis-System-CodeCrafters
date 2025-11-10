package command.handlers.geospatial;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class GEODISTHandler implements CommandStrategy {
    private final DataStore dataStore;

    public GEODISTHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String member1 = arguments.get(1);
            String member2 = arguments.get(2);
            double member1Score = dataStore.zscore(key, member1);
            double member2Score = dataStore.zscore(key, member2);
            if (member1Score == -1 || member2Score == -1) {
                clientOutput.write("$-1\r\n"); // Null bulk string
            } else {
                double distance = Math.abs(member2Score - member1Score);
                clientOutput.write(RESPSerializer.bulkString(String.valueOf(distance))); // Simple string response
            }
            clientOutput.flush();
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEODIST command.");
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 3) {
            throw new IllegalArgumentException("GEODIST requires exactly 3 arguments: key, member1, member2.");
        }
    }

}
