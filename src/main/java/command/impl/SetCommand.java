package command.impl;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SetCommand implements CommandStrategy {
    private final DataStore dataStore;

    public SetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 2) {
                clientOutput.write(RESPSerializer.error("wrong number of arguments for 'set' command"));
                clientOutput.flush();
                return;
            }

            String key = arguments.get(0);
            String value = arguments.get(1);
            dataStore.set(key, value);
            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
