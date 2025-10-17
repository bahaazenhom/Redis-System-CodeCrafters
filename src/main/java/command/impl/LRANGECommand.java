package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
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
                clientOutput.write(RESPSerializer.emptyArray());
                clientOutput.flush();
                return;
            }
            Deque<String> values = ((ListValue) dataStore.getValue(listName)).getList();

            int size = values.size();
            if (startIndex < 0) startIndex += size;
            if (stopIndex < 0) stopIndex += size;
            if (startIndex < 0) startIndex = 0;
            if (stopIndex < 0) stopIndex = 0;

            if (startIndex >= values.size() || startIndex > stopIndex) {
                clientOutput.write(RESPSerializer.emptyArray());
                clientOutput.flush();
                return;
            }
            if (stopIndex >= values.size())
                stopIndex = values.size() - 1;

            List<String> listValues = new ArrayList<>(values).subList(startIndex, stopIndex + 1);
            clientOutput.write(RESPSerializer.array(listValues));
            clientOutput.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
