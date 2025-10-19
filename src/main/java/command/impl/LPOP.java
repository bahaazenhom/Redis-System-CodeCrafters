package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class LPOP implements CommandStrategy {
    private final DataStore dataStore;

    public LPOP(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() < 1) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'LPOP' command"));
                clientOutput.flush();
                return;
            }
            String listName = arguments.get(0);
            Long counter = arguments.size() > 1 ? Long.parseLong(arguments.get(1)) : null;
            if (!dataStore.exists(listName)) {
                clientOutput.write(RESPSerializer.nullBulkString());
                clientOutput.flush();
                return;
            }
            List<String> firstValues = dataStore.lpop(listName, counter);
            if (counter == null) {
                clientOutput.write(RESPSerializer.bulkString(firstValues.get(0)));
            } else {
                clientOutput.write(RESPSerializer.array(firstValues));
            }
            clientOutput.flush();

        } catch (IOException exception) {
            throw new RuntimeException();
        }
    }

}
