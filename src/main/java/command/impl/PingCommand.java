package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class PingCommand implements CommandStrategy {
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
