package command.handlers.connection;

import command.CommandStrategy;
import server.connection.ClientConnection;

import java.util.List;

public class ACLWhoAmIHandler implements CommandStrategy {
    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String username = clientOutput.getUsername();
            clientOutput.write(username);
            clientOutput.flush();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {

    }
}

