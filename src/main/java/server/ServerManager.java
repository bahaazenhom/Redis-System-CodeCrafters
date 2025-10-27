package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.CommandExecuter;

/**
 * ServerManager handles multiple Redis server instances running on different
 * ports.
 * Each server runs in its own thread and shares the same CommandExecuter and
 * DataStore.
 */
public class ServerManager {
    private static final Map<Integer, ServerInstance> runningServers = new ConcurrentHashMap<>();
    private final CommandExecuter commandExecuter;
    private static ServerManager serverManager;

    private ServerManager(CommandExecuter commandExecuter) {
        this.commandExecuter = commandExecuter;
    }

    public static ServerManager create(CommandExecuter commandExecuter) {
        if (serverManager != null) {
            return serverManager;
        }
        serverManager = new ServerManager(commandExecuter);
        return serverManager;
    }

    /**
     * Start a new server on the specified port
     * 
     * @param port The port number to bind to
     * @return true if server started successfully, false if port is already in use
     */
    public boolean startServer(int port, String serverRole) {
        if (runningServers.containsKey(port)) {
            System.out.println("Server already running on port: " + port);
            return false;
        }

        try {
            ServerInstance instance = new ServerInstance(port, commandExecuter, serverRole);
            runningServers.put(port, instance);

            Thread serverThread = new Thread(instance, "Server-" + port);
           // serverThread.setDaemon(true); // Daemon thread so it doesn't prevent JVM shutdown
            serverThread.start();

            System.out.println("Successfully started server on port: " + port);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            return false;
        }
    }

    public String getServerRole(int port) {
        return runningServers.get(port).getServerRole();
    }

    /**
     * Stop a server running on the specified port
     * 
     * @param port The port number of the server to stop
     * @return true if server was stopped, false if no server was running on that
     *         port
     */
    public boolean stopServer(int port) {
        ServerInstance instance = runningServers.remove(port);
        if (instance != null) {
            instance.stop();
            System.out.println("Stopped server on port: " + port);
            return true;
        }
        return false;
    }

    /**
     * Check if a server is running on the specified port
     */
    public boolean isServerRunning(int port) {
        return runningServers.containsKey(port);
    }

    /**
     * Get all running server ports
     */
    public java.util.Set<Integer> getRunningPorts() {
        return runningServers.keySet();
    }

    /**
     * Inner class representing a single server instance
     */
    private static class ServerInstance implements Runnable {
        private final int port;
        private final CommandExecuter commandExecuter;
        private final ServerSocket serverSocket;
        private volatile boolean running = true;
        private final String serverRole;

        public String getServerRole() {
            return serverRole;
        }

        public ServerInstance(int port, CommandExecuter commandExecuter, String serverRole) throws IOException {
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
                            new ClientHandler(clientSocket, commandExecuter, serverRole),
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
    }
}
