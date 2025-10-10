
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import command.CommandExecuter;
import server.ClientHandler;
import storage.impl.InMemoryDataStore;

public class Main {
    public static void main(String[] args) throws IOException {
        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        System.out.println("Logs from your program will appear here!");
        int port = 6379;
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

}
