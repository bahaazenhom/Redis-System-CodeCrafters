package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;

public class INFOCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.get(0).equals("replication")) {
                clientOutput.write(RESPSerializer.bulkString("role:master"));
                clientOutput.flush();
            }
        } catch (NumberFormatException e) {
            try {
                clientOutput.write("Invalid port number\n");
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        try {
            if (arguments.size() != 1) {
                throw new IllegalArgumentException("Wrong number of arguments for 'INFO' command");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid arguments for 'INFO' command");
        }
    }

}
