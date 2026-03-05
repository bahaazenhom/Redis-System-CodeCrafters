package server.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServerManager handles multiple Redis server instances running on different
 * ports.
 * Each server runs in its own thread and shares the same CommandExecuter and
 * DataStore.
 */
public class ServerManager {
    private static final Map<Integer, ServerInstance> runningServers = new ConcurrentHashMap<>();
    private static ServerManager serverManager;

    private ServerManager() {
    }

    public static ServerManager create() {
        if (serverManager != null) {
            return serverManager;
        }
        serverManager = new ServerManager();
        return serverManager;
    }

    /**
     * Start a new server on the specified port
     * 
     * @param serverInstance The server instance to start
     * @return true if server started successfully, false if port is already in use
     */
    public boolean startServer(ServerInstance serverInstance) {
        if (runningServers.containsKey(serverInstance.getPort())) {
            return false;
        }

        runningServers.put(serverInstance.getPort(), serverInstance);

        Thread serverThread = new Thread(serverInstance, "Server-" + serverInstance.getPort());
        serverThread.start();

        return true;
    }

    /**
     * Stop a server running on the specified port
     * 
     * @param serverInstance The server instance to stop
     * @return true if server was stopped, false if no server was running on that
     *         port
     */
    public boolean stopServer(ServerInstance serverInstance) {
        int port = serverInstance.getPort();
        ServerInstance instance = runningServers.remove(port);
        if (instance != null) {
            instance.stop();
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
    public Set<Integer> getRunningPorts() {
        return runningServers.keySet();
    }

}
