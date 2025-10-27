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
            int port = Integer.parseInt(arguments.get(0));
            if (arguments.size() == 1) {
                clientOutput.write("Server is running on port: " + port + "\n");
                clientOutput.flush();
            } else if (arguments.get(1).equals("replication")) {
                clientOutput.write(RESPSerializer.bulkString("roler:master"));
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
            if (arguments.size() < 1 || arguments.size() > 2) {
                throw new IllegalArgumentException("Wrong number of arguments for 'INFO' command");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid arguments for 'INFO' command");
        }
    }

}
