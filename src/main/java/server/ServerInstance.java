package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import command.CommandExecuter;

public class ServerInstance implements Runnable {
    private final String host;

    private final int port;
    private final CommandExecuter commandExecuter;
    private final ServerSocket serverSocket;
    private volatile boolean running = true;
    private final String serverRole;

    public String getServerRole() {
        return serverRole;
    }

    public ServerInstance(String host, int port, CommandExecuter commandExecuter, String serverRole)
            throws IOException {
        this.host = host;
        this.port = port;
        this.commandExecuter = commandExecuter;
        this.serverSocket = new ServerSocket(port);
        this.serverRole = serverRole;
        this.serverSocket.setReuseAddress(true);
    }

    @Override
    public void run() {
        System.out.println("Server started on port " + port + ", waiting for clients...");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected to port " + port + ": " + clientSocket.getInetAddress());
                // Handle each client in a separate thread
                Thread clientThread = new Thread(
                        new ClientHandler(clientSocket, commandExecuter),
                        "Client-" + port + "-" + clientSocket.getPort());
                clientThread.start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection on port " + port + ": " + e.getMessage());
                }
                // If not running, this is expected during shutdown
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server socket on port " + port + ": " + e.getMessage());
        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}