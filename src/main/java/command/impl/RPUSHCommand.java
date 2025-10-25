package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.types.ListValue;

public class RPUSHCommand implements CommandStrategy {
    private final DataStore dataStore;

    public RPUSHCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("wrong number of arguments for 'RPUSH' command");
        }
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            String listName = arguments.get(0);
            List<String> values = arguments.subList(1, arguments.size());
            
            if (!dataStore.exists(listName))
                dataStore.setValue(listName, new ListValue(new ArrayDeque<>()));
            if (!(dataStore.getValue(listName) instanceof ListValue)) {
                clientOutput.write(RESPSerializer.error("WRONGTYPE Operation against a key holding the wrong kind of value"));
                clientOutput.flush();
                return;
            }
            long size = dataStore.rpush(listName, values);
            clientOutput.write(RESPSerializer.integer(size));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
