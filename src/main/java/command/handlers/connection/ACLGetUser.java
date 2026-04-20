package command.handlers.connection;

import command.CommandStrategy;
import protocol.RESPParser;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

import java.util.List;
import java.util.Map;

public class ACLGetUser implements CommandStrategy {
    private final DataStore dataStore;

    public ACLGetUser(DataStore dataStore) {
        this.dataStore = dataStore;
    }


    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String userName = arguments.get(0);
            Map<String, List<String>> userProperties = (Map<String, List<String>>) dataStore.getUserProperties(userName).getValue();
            clientOutput.write(RESPSerializer.writeUserProperties(userProperties));
            clientOutput.flush();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size()!=1){
            throw new IllegalArgumentException("Wrong number of arguments for 'ACL GETUSER' command");
        }
    }
}
