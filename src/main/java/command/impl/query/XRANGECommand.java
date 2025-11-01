package command.impl.query;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class XRANGECommand implements CommandStrategy {
    private final DataStore dataStore;

    public XRANGECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 3) {
            throw new IllegalArgumentException("Wrong number of arguments for 'XRANGE' Command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String streamKey = arguments.get(0);
            String startEntryId = arguments.get(1);
            String endEntryId = arguments.get(2);
            List<List<Object>> result = dataStore.XRANGE(streamKey, startEntryId, endEntryId, true);
            clientOutput.write(RESPSerializer.xRangeArray(result));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
