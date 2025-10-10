package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class GetCommand implements CommandStrategy {
    private final DataStore dataStore;

    public GetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 1) {
                clientOutput.write(RESPSerializer.error("wrong number of arguments for 'get' command"));
                clientOutput.flush();
                return;
            }
            String key = arguments.get(0);
            String value = dataStore.get(key);
            if (value != null) {
                clientOutput.write(RESPSerializer.bulkString(value));
            } else {
                clientOutput.write(RESPSerializer.bulkString(null)); // Null bulk reply
            }
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
