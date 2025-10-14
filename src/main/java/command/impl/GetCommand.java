package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.model.concreteValues.StringValue;

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
            var redisValue = dataStore.getValue(key);
            
            if (redisValue == null) {
                clientOutput.write(RESPSerializer.nullBulkString()); // Null bulk reply
                clientOutput.flush();
                return;
            }
            
            String value = ((StringValue) redisValue).getString();
            clientOutput.write(RESPSerializer.bulkString(value));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
