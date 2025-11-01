
import java.io.IOException;
import java.util.logging.Level;

import command.CommandExecuter;
import replication.ReplicationManager;
import server.ServerInstance;
import server.ServerManager;
import storage.impl.InMemoryDataStore;
import util.AppLogger;

public class Main {
    private static final int DEFAULT_PORT = 6379;

    public static void main(String[] args) throws IOException {
        // Initialize logging system
        AppLogger.initialize();
        // Set log level to INFO for production, or FINE/FINEST for debugging
        AppLogger.setLogLevel(Level.INFO); // Change to Level.FINE for more verbose logs

        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        ServerManager serverManager = ServerManager.create();
        int port = parsePort(args);
        String[] serverRole = parseServerRole(args);
        ReplicationManager replicationManager = ReplicationManager.create();
        ServerInstance serverInstance = replicationManager.createReplica(port, commandExecuter, serverRole);
        serverManager.startServer(serverInstance);
    }

    private static String[] parseServerRole(String[] args) {
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (arg.equals("--replicaof")) {
                String[] masterData = args[index + 1].split(" ");
                return new String[] { "slave", masterData[0], masterData[1] };
            }
        }
        return new String[] { "master" }; // default state
    }

    private static int parsePort(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                if (args[0].equals("--port")) {
                    port = Integer.parseInt(args[1]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
            }
        }
        return port;
    }
}
