
import java.io.IOException;
import java.util.logging.Level;

import command.CommandExecuter;
import replication.ReplicationManager;
import server.ServerContext;
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

        ServerConfiguration config = new ServerConfiguration(args);

        ServerContext serverContext = ServerContext.getInstance();
        
        // Store configuration in ServerContext for global access
        serverContext.setConfiguration(config);

        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        ServerManager serverManager = ServerManager.create();

        int port = config.getPort();
        int masterPort = config.getMasterPort();
        String serverRole = config.getServerRole();
        String masterHost = config.getMasterHost();

        ReplicationManager replicationManager = ReplicationManager.create();
        ServerInstance serverInstance = replicationManager.createReplica(commandExecuter, port, serverRole, masterPort, masterHost);
        serverManager.startServer(serverInstance);
    }


}
