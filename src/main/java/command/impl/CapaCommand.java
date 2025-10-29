package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;

public class CapaCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            // Currently, no capabilities are supported, so we return an empty list
            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size() != 1){
            throw new IllegalArgumentException("Wrong number of arguments for 'CAPA' command");
        }
    }

    
}
