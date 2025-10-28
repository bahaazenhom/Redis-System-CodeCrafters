package command.impl;

import command.CommandStrategy;
import command.ResponseWriter.ResponseWriter;
import protocol.RESPSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class EchoCommand implements CommandStrategy {

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("wrong number of arguments for 'echo' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ResponseWriter clientOutput) {
        try {
            String message = arguments.get(0);
            clientOutput.write(RESPSerializer.bulkString(message));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
