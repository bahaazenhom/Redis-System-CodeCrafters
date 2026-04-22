package command.handlers.authentication;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

import java.util.List;

public class ACLSetUser implements CommandStrategy {
    private final DataStore store;

    public ACLSetUser(DataStore dataStore){
        this.store = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String userName = arguments.get(0);
            String password = arguments.get(1);
            boolean isAdded = store.addUserPassword(userName, password);
            if(isAdded) clientOutput.write(RESPSerializer.simpleString("OK"));
            else clientOutput.write(RESPSerializer.error("WRONGPASS invalid username or user is disabled."));
            clientOutput.flush();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'ACL SETUSER' command");
        }
    }
}
