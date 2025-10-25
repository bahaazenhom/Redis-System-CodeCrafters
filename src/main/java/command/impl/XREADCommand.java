package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class XREADCommand implements CommandStrategy {
    private final DataStore dataStore;

    public XREADCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() < 3) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'XREAD' Command"));
                clientOutput.flush();
                return;
            }

            Long timestamp = 0L;
            boolean block = false;
            if (arguments.contains("block")) {
                block = true;
                int blockIndex = arguments.indexOf("block");
                timestamp = Long.parseLong(arguments.get(blockIndex + 1));
            }

            int streamsIndexStart = arguments.indexOf("streams");
            int numStreams = (arguments.size() - streamsIndexStart - 1) / 2;

            int IDsIndexStart = streamsIndexStart + 1 + numStreams;
            List<String> streamsKeys = arguments.subList(streamsIndexStart + 1, streamsIndexStart + 1 + numStreams);
            List<String> streamsStartEntriesIDs = arguments.subList(IDsIndexStart, arguments.size());

            try {
                List<List<Object>> result = dataStore.XREAD(streamsKeys, streamsStartEntriesIDs, block, timestamp);
                clientOutput.write(RESPSerializer.xReadArray(result));
                clientOutput.flush();
            } catch (InterruptedException e) {
                clientOutput.write(RESPSerializer.error(e.getMessage()));
                clientOutput.flush();
                return;
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }
}