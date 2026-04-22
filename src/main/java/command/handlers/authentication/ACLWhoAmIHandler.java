package command.handlers.authentication;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.entity.ClientConnection;

import java.util.List;

public class ACLWhoAmIHandler implements CommandStrategy {


    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String username = clientOutput.getUsername();
            clientOutput.write(RESPSerializer.bulkString(username));

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

