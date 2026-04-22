package command.handlers.authentication;

import command.CommandStrategy;
import protocol.RESPSerializer;
import protocol.errorenum.ErrorType;
import server.connection.entity.ClientConnection;
import storage.DataStore;
import util.AppLogger;

import java.util.List;
import java.util.logging.Logger;

public class ACLSetUser implements CommandStrategy {
    private final DataStore store;
    Logger logger = AppLogger.getLogger(ACLSetUser.class);

    public ACLSetUser(DataStore dataStore){
        this.store = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            logger.info("Executing ACL SETUSER with arguments: " + arguments);
            String userName = arguments.get(0);
            String password = arguments.get(1);
            boolean isPasswordSet = store.setUserPassword(userName, password);
            if(isPasswordSet) clientOutput.write(RESPSerializer.simpleString("OK"));
            else clientOutput.write(RESPSerializer.error(ErrorType.NOAUTH,"Authentication required."));
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
