package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class PingCommand implements CommandStrategy {
    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        // PING command accepts 0 or 1 arguments (optional message)
        if (arguments.size() > 1) {
            throw new IllegalArgumentException("wrong number of arguments for 'ping' command");
        }
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            clientOutput.write(RESPSerializer.simpleString("PONG"));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
