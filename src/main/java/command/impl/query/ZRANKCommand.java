package command.impl.query;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class ZRANKCommand implements CommandStrategy {
    private final DataStore dataStore;

    public ZRANKCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String key = arguments.get(0);
            String memberName = arguments.get(1);
            int rank = dataStore.zrank(key, memberName);

            clientOutput.write(RESPSerializer.integer(rank));
            clientOutput.flush();
        }
        catch (IOException e){
            throw new RuntimeException("IO Error during command execution", e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("ZRANK command requires exactly 2 arguments: key and member name.");
        }
    }

}
