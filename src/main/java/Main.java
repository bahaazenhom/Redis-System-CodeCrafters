
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import command.CommandExecuter;
import server.ClientHandler;
import storage.impl.InMemoryDataStore;

public class Main {
    private static final int DEFAULT_PORT = 6379;
    public static void main(String[] args) throws IOException {
        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        System.out.println("Logs from your program will appear here!");
        int port = parsePort(args); // Default Redis port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            // Continuously accept new clients
            while (true) {
                System.out.println("Waiting for clients to connect...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                new Thread(new ClientHandler(clientSocket, commandExecuter)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
    private static int parsePort(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try{
                if(args[0].equals("--port")){
                    port = Integer.parseInt(args[1]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
            }
        }
        return port;
    }
}
