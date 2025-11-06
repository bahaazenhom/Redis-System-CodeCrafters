package command.impl.query;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import rdb.RDBManager;
import server.connection.ClientConnection;
import server.core.ServerContext;
import storage.DataStore;
import util.ServerConfiguration;

public class KEYCommand implements CommandStrategy {
    private final DataStore dataStore;

    public KEYCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String pattern = arguments.get(0);
            
            // Get RDB file configuration
            ServerContext context = ServerContext.getInstance();
            ServerConfiguration config = context.getConfiguration();
            
            if (config == null) {
                clientOutput.write(RESPSerializer.error("ERR server configuration not initialized"));
                clientOutput.flush();
                return;
            }
            
            String directory = config.getRdbFileDir();
            String filename = config.getRdbFileName();
            
            if (directory == null || filename == null) {
                clientOutput.write(RESPSerializer.error("ERR RDB file not configured"));
                clientOutput.flush();
                return;
            }
            
            // Search RDB file for matching keys
            RDBManager rdbManager = new RDBManager(dataStore, directory, filename);
            List<String> keys = rdbManager.findKeysInRDBFile(pattern);
            
            // Return array of keys in RESP format
            clientOutput.write(RESPSerializer.array(keys));
            clientOutput.flush();
            
        } catch (IOException e) {
            try {
                clientOutput.write(RESPSerializer.error("ERR " + e.getMessage()));
                clientOutput.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Error writing response", ex);
            }
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'KEYS' command");
        }
    }
}
