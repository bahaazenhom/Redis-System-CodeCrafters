
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import command.CommandExecuter;
import rdb.RDBException;
import rdb.RDBManager;
import replication.ReplicationManager;
import server.core.ServerContext;
import server.core.ServerInstance;
import server.core.ServerManager;
import storage.DataStore;
import storage.InMemoryDataStore;
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
        System.out.println(Arrays.toString(args));
        AppLogger.getLogger(Main.class.getName()).info("Starting server with configuration: " + config.toString());

        // Initialize ServerContext singleton
        ServerContext serverContext = ServerContext.getInstance();
        
        // Store configuration in ServerContext for global access
        serverContext.setConfiguration(config);

        // Log server startup information
        DataStore dataStore = new InMemoryDataStore();
        
        // Load RDB file if configured
        String rdbFileDir = config.getRdbFileDir();
        String rdbFileName = config.getRdbFileName();
        if (rdbFileDir != null && rdbFileName != null && !rdbFileDir.isEmpty() && !rdbFileName.isEmpty()) {
            try {
                RDBManager rdbManager = new RDBManager(dataStore, rdbFileDir, rdbFileName);
                if (rdbManager.fileExists()) {
                    AppLogger.getLogger(Main.class.getName()).info("Loading RDB file: " + rdbManager.getFilePath());
                    rdbManager.load();
                    AppLogger.getLogger(Main.class.getName()).info("RDB file loaded successfully");
                } else {
                    AppLogger.getLogger(Main.class.getName()).info("RDB file not found, starting with empty database");
                }
            } catch (RDBException e) {
                AppLogger.getLogger(Main.class.getName()).severe("Failed to load RDB file: " + e.getMessage());
                // Continue with empty database
            }
        }
        
        CommandExecuter commandExecuter = new CommandExecuter(dataStore);
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
