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
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() < 2) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'LPUSH' command"));
                clientOutput.flush();
                return;
            }
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
