package command.handlers.connection;

import command.CommandStrategy;
import domain.values.UserProperties;
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
            System.out.println("Executing ACL GETUSER for user: " + userName);
            Map<String, List<String>> userProperties = dataStore.getUserProperties(userName).getValue();
            System.out.println("Retrieved user properties for " + userName + ": " + userProperties);
           // clientOutput.write(RESPSerializer.bulkString(userName));
            clientOutput.write(RESPSerializer.writeUserProperties(userProperties));
            clientOutput.flush();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        // I want to use logging here
        System.out.println("Validating arguments for ACL GETUSER command: " + arguments);
        if(arguments.size()!=1){
            throw new IllegalArgumentException("Wrong number of arguments for 'ACL GETUSER' command");
        }
    }
}
