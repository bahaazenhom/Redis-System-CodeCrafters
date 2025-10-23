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
            int streamsIndex = arguments.indexOf("STREAMS");
            if (streamsIndex == -1)
                streamsIndex = arguments.indexOf("streams");

            if (streamsIndex == -1 || streamsIndex + 1 >= arguments.size()) {
                clientOutput.write(RESPSerializer.error("Missing STREAMS keyword or stream keys"));
                clientOutput.flush();
                return;
            }
            int numStreams = (arguments.size() - streamsIndex - 1) / 2;
            List<String> streamsKeys = arguments.subList(streamsIndex + 1, streamsIndex + 1 + numStreams);
            List<String> streamsStartEntriesIDs = arguments.subList(streamsIndex + 1 + numStreams, arguments.size());

            List<List<Object>> result = dataStore.XREAD(streamsKeys, streamsStartEntriesIDs);
            clientOutput.write(RESPSerializer.XReadArray(result));
            clientOutput.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }
}