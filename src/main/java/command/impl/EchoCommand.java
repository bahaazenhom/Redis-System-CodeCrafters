package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class EchoCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 1) {
                clientOutput.write(RESPSerializer.error("wrong number of arguments for 'echo' command"));
                clientOutput.flush();
                return;
            }
            String message = arguments.get(0);
            clientOutput.write(RESPSerializer.bulkString(message));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
