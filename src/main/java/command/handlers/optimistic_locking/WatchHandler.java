package command.handlers.optimistic_locking;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.entity.ClientConnection;

import java.util.List;

public class WatchHandler implements CommandStrategy{
    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String key = arguments.get(0);
            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.isEmpty())throw new IllegalArgumentException("Wrong number of arguments for 'WATCH' command");
    }
}
