package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.ServerManager;

public class INFOCommand implements CommandStrategy {
    private final ServerManager serverManager;

    public INFOCommand(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.get(0).equals("replication")) {
                int port = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);
                String role = serverManager.getServerRole(port);
                clientOutput.write(RESPSerializer.bulkString("role:" + role));
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
