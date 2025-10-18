package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class TYPECommand implements CommandStrategy {
    private final DataStore dataStore;

    public TYPECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 1) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'TYPE' command"));
                clientOutput.flush();
                return;
            }
            String key = arguments.get(0);
            if (!dataStore.exists(key))
                clientOutput.write(RESPSerializer.bulkString("none"));
            else {
                String type = dataStore.getType(key).toString().toLowerCase();
                clientOutput.write(RESPSerializer.bulkString(type));
            }
            clientOutput.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
