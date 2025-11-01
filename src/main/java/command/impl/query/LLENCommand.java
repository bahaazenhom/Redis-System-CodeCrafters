package command.impl.query;

import java.util.Deque;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;
import storage.types.ListValue;

public class LLENCommand implements CommandStrategy {
    private final DataStore dataStore;

    public LLENCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LLEN' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
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
