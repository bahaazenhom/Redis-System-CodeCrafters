package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import command.CommandExecuter;
import command.CommandStrategy;
import server.ClientHandler;
import storage.impl.InMemoryDataStore;

public class PORTCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        String portNumber = arguments.get(0);
        int port = Integer.parseInt(portNumber);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true){
                Socket socket = serverSocket.accept();
                // Handle the client connection (e.g., create a new thread to manage communication)
                new Thread(new ClientHandler(socket, new CommandExecuter(new InMemoryDataStore()))).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        

    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'PORT' command");
        }
        String portNumber = arguments.get(0);
        try {
            int port = Integer.parseInt(portNumber);
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port number must be between 0 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port number must be a valid integer");
        }
    }

}
