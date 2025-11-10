package command.handlers.transaction;

import command.CommandStrategy;
import protocol.RESPSerializer;
import rdb.RDBException;
import rdb.RDBManager;
import server.connection.ClientConnection;
import server.core.ServerContext;
import storage.DataStore;
import util.ServerConfiguration;

import java.io.IOException;
import java.util.List;

public class SAVEHandler implements CommandStrategy {
    
    private final DataStore dataStore;
    
    public SAVEHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    @Override
    public void execute(List<String> arguments, ClientConnection clientConnection) {
        try {
            // Get configuration from ServerContext
            ServerConfiguration config = ServerContext.getInstance().getConfiguration();
            if (config == null) {
                clientConnection.write(RESPSerializer.error("ERR server configuration not initialized"));
                clientConnection.flush();
                return;
            }
            
            // Get RDB file location from config
            String directory = config.getRdbFileDir();
            String filename = config.getRdbFileName();
            
            if (directory == null || directory.trim().isEmpty()) {
                clientConnection.write(RESPSerializer.error("ERR RDB directory not configured"));
                clientConnection.flush();
                return;
            }
            if (filename == null || filename.trim().isEmpty()) {
                clientConnection.write(RESPSerializer.error("ERR RDB filename not configured"));
                clientConnection.flush();
                return;
            }
            
            // Create RDBManager and perform save
            RDBManager rdbManager = new RDBManager(dataStore, directory, filename);
            rdbManager.save();
            
            clientConnection.write(RESPSerializer.simpleString("OK"));
            clientConnection.flush();
            
        } catch (IOException e) {
            try {
                clientConnection.write(RESPSerializer.error("ERR failed to save RDB file: " + e.getMessage()));
                clientConnection.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Error writing response", ex);
            }
        } catch (RDBException e) {
            try {
                clientConnection.write(RESPSerializer.error("ERR RDB error: " + e.getMessage()));
                clientConnection.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Error writing response", ex);
            }
        } catch (Exception e) {
            try {
                clientConnection.write(RESPSerializer.error("ERR unexpected error: " + e.getMessage()));
                clientConnection.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Error writing response", ex);
            }
        }
    }
    
    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        // SAVE takes no arguments
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException("Wrong number of arguments for 'SAVE' command");
        }
    }
}
