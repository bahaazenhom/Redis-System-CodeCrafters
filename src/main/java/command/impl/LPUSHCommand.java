package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.types.ListValue;

public class LPUSHCommand implements CommandStrategy {
    private final DataStore dataStore;

    public LPUSHCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LPUSH' command");
        }
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            String listName = arguments.get(0);
            List<String> values = arguments.subList(1, arguments.size());
            if (!dataStore.exists(listName))
                dataStore.setValue(listName, new ListValue(new ArrayDeque<>()));
            long size = dataStore.lpush(listName, values);
            clientOutput.write(RESPSerializer.integer(size));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
