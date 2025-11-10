package command.handlers.connection;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;

import java.io.IOException;
import java.util.List;

public class EchoHandler implements CommandStrategy {

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("wrong number of arguments for 'echo' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String message = arguments.get(0);
            clientOutput.write(RESPSerializer.bulkString(message));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
