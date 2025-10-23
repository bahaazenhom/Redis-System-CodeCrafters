package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class XRANGECommand implements CommandStrategy {
    private final DataStore dataStore;

    public XRANGECommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 3) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'XRANGE' Command"));
                clientOutput.flush();
                return;
            }
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
