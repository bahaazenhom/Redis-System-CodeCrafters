package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.model.concreteValues.ListValue;

public class LRANGECommand implements CommandStrategy {
    private final DataStore dataStore;

    public LRANGECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() < 3) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'LRANGE' command"));
                clientOutput.flush();
            }
            String listName = arguments.get(0);
            int startIndex = Integer.parseInt(arguments.get(1));
            int stopIndex = Integer.parseInt(arguments.get(2));

            

            if (!dataStore.exists(listName)) {
                clientOutput.write(RESPSerializer.array(null));
                clientOutput.flush();
                return;
            }
            List<String> values = ((ListValue) dataStore.getValue(listName)).getList();

            startIndex = startIndex < 0 ? Math.max(0, values.size() + startIndex) : startIndex;
            stopIndex = stopIndex < 0 ? Math.max(0, values.size() + stopIndex) : stopIndex;
            
            if (startIndex >= values.size() || startIndex > stopIndex) {
                clientOutput.write(RESPSerializer.array(null));
                clientOutput.flush();
                return;
            }
            if (stopIndex >= values.size())
                stopIndex = values.size() - 1;

            clientOutput.write(RESPSerializer.array(values.subList(startIndex, stopIndex + 1)));
            clientOutput.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
