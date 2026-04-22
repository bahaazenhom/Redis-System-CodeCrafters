package command.handlers.authentication;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.entity.ClientConnection;
import storage.DataStore;

import java.util.List;

public class AUTHHandler implements CommandStrategy {
    private final DataStore store;

    public AUTHHandler(DataStore dataStore){
        this.store = dataStore;
    }
    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String userName = arguments.get(0);
            String password = arguments.get(1);
            boolean authedUser = store.authenticateUser(userName, password);
            if(authedUser)clientOutput.write(RESPSerializer.simpleString("OK"));
            else clientOutput.write(RESPSerializer.wrongPass("invalid username-password pair or user is disabled."));
            clientOutput.flush();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size() != 2){
            throw new IllegalArgumentException("Wrong number of arguments for 'AUTH' command");
        }
    }
}
