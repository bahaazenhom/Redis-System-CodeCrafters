
import java.io.IOException;
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


    public static void main(String[] args) {
        try {
            // Initialize logging system
            AppLogger.initialize();
            AppLogger.setLogLevel(Level.INFO); // Change to Level.FINE for more verbose logs

            var logger = AppLogger.getLogger(Main.class.getName());
            logger.info("=== Redis Server Starting ===");

            // Parse and validate configuration
            ServerConfiguration config = new ServerConfiguration(args);
            logger.info("Configuration loaded: " + config.toString());

            // Initialize ServerContext singleton
            ServerContext serverContext = ServerContext.getInstance();
            serverContext.setConfiguration(config);

            // Create core components
            ServerManager serverManager = ServerManager.create();
            ReplicationManager replicationManager = ReplicationManager.create();

            // Initialize in-memory data store
            DataStore dataStore = new InMemoryDataStore();
            serverContext.setDataStore(dataStore);

            // Load RDB file if configured
            loadRDBFile(dataStore, config, logger);

            // Create core components
            CommandExecuter commandExecuter = new CommandExecuter(dataStore);

            // Create and start server instance
            logger.info("Creating server instance on port " + config.getPort());
            ServerInstance serverInstance = replicationManager.createReplica(
                commandExecuter,
                config.getPort(),
                config.getServerRole(),
                config.getMasterPort(),
                config.getMasterHost()
            );

            // Register shutdown hook for graceful shutdown
            registerShutdownHook(serverManager, serverInstance, logger);

            // Start the server
            logger.info("Starting server...");
            serverManager.startServer(serverInstance);
            logger.info("=== Redis Server Started Successfully ===");

        } catch (IOException e) {
            AppLogger.getLogger(Main.class.getName()).severe("I/O error during server initialization: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            AppLogger.getLogger(Main.class.getName()).severe("Unexpected error during server initialization: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load RDB file if configured and exists
     */
    private static void loadRDBFile(DataStore dataStore, ServerConfiguration config, java.util.logging.Logger logger) {
        String rdbFileDir = config.getRdbFileDir();
        String rdbFileName = config.getRdbFileName();

        if (rdbFileDir == null || rdbFileName == null || rdbFileDir.isEmpty() || rdbFileName.isEmpty()) {
            return;
        }

        try {
            RDBManager rdbManager = new RDBManager(dataStore, rdbFileDir, rdbFileName);
            if (rdbManager.fileExists()) {
                logger.info("Loading RDB file: " + rdbManager.getFilePath());
                rdbManager.load();
                logger.info("RDB file loaded successfully");
            } else {
                logger.info("RDB file not found, starting with empty database");
            }
        } catch (RDBException e) {
            logger.severe("Failed to load RDB file: " + e.getMessage());
            logger.info("Continuing with empty database");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register shutdown hook for graceful server shutdown
     */
    private static void registerShutdownHook(ServerManager serverManager, ServerInstance serverInstance, java.util.logging.Logger logger) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("=== Redis Server Shutting Down ===");
            try {
                serverManager.stopServer(serverInstance);
                logger.info("Server stopped gracefully");
            } catch (Exception e) {
                logger.severe("Error during server shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }


}
