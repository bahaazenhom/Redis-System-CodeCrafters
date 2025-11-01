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
    private Socket clientSocket;

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
        while (running) {
            try {
                clientSocket = serverSocket.accept();
                // Handle each client in a separate thread
                Thread clientThread = new Thread(
                        new ClientHandler(clientSocket, commandExecuter),
                        "Client-" + port + "-" + clientSocket.getPort());
                clientThread.start();

            } catch (IOException e) {
                // Expected during shutdown
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Error closing socket
        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public CommandExecuter getCommandExecuter() {
        return commandExecuter;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}