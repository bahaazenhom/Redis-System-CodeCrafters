package command.impl;

import java.io.BufferedWriter;
import java.util.Deque;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.types.ListValue;

public class LLEN implements CommandStrategy {
    private final DataStore dataStore;

    public LLEN(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 1) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'LLEN' command"));
                clientOutput.flush();
                return;
            }
            String listName = arguments.get(0);
            if (!dataStore.exists(listName)) {
                clientOutput.write(RESPSerializer.integer(0));
                clientOutput.flush();
                return;
            }
            Deque<String> values = ((ListValue) dataStore.getValue(listName)).getList();
            clientOutput.write(RESPSerializer.integer(values.size()));
            clientOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
