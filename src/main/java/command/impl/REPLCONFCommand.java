package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;

public class REPLCONFCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            // REPLCONF command does not require any specific action in this implementation
            // It is typically used for replication configuration between master and replica
            System.out.println("the simple string is here --------------------------");
            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size() < 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'REPLCONF' command");
        }
    }
    
}
