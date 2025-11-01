package command.impl.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.types.ListValue;

public class LRANGECommand implements CommandStrategy {
    private final DataStore dataStore;

    public LRANGECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LRANGE' command");
        }
        try {
            Integer.parseInt(arguments.get(1));
            Integer.parseInt(arguments.get(2));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("value is not an integer or out of range");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
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
