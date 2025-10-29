package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;

public class REPLCONFACKCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String ackValue = arguments.get(0);
            // Currently, no specific action is taken with the ACK value.
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'REPLCONF ACK' command");
        }
    }

    
}
