
import java.io.IOException;
import java.util.logging.Level;

import command.CommandExecuter;
import replication.ReplicationManager;
import server.core.ServerContext;
import server.core.ServerInstance;
import server.core.ServerManager;
import storage.impl.InMemoryDataStore;
import util.AppLogger;
import util.ServerConfiguration;

public class Main {

    public static void main(String[] args) throws IOException {
        // Initialize logging system
        AppLogger.initialize();

        // Set log level to INFO for production, or FINE/FINEST for debugging
        AppLogger.setLogLevel(Level.INFO); // Change to Level.FINE for more verbose logs

        //AppLogger.setLogLevel(Level.FINEST); // Uncomment for maximum verbosity during debugging
        ServerConfiguration config = new ServerConfiguration(args);

        // Initialize ServerContext singleton
        ServerContext serverContext = ServerContext.getInstance();
        
        // Store configuration in ServerContext for global access
        serverContext.setConfiguration(config);

        // Log server startup information
        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        ServerManager serverManager = ServerManager.create();

        // Extract configuration parameters
        int port = config.getPort();
        int masterPort = config.getMasterPort();
        String serverRole = config.getServerRole();
        String masterHost = config.getMasterHost();

        // Create and start server instance
        ReplicationManager replicationManager = ReplicationManager.create();
        ServerInstance serverInstance = replicationManager.createReplica(commandExecuter, port, serverRole, masterPort, masterHost);
        serverManager.startServer(serverInstance);
    }


}
