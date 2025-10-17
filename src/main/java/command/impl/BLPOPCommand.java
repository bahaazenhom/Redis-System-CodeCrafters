package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;

public class BLPOPCommand implements CommandStrategy {
    private final DataStore dataStore;

    public BLPOPCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            if (arguments.size() != 2) {
                clientOutput.write(RESPSerializer.error("Wrong number of arguments for 'BLPOP' command"));
                clientOutput.flush();
                return;
            }
            String listKey = arguments.get(0);
            double timestamp;
            try {
                timestamp = Double.parseDouble(arguments.get(1));
                if (timestamp < 0) {
                    clientOutput.write(RESPSerializer.error("timeout is negative"));
                    clientOutput.flush();
                    return;
                }
            } catch (NumberFormatException e) {
                clientOutput.write(RESPSerializer.error("timeout is not a number or out of range"));
                clientOutput.flush();
                return;
            }

            String value = dataStore.BLPOP(listKey, timestamp);

            List<String> result = new ArrayList<>();
            result.add(listKey);
            result.add(value);

            if (value == null)
                clientOutput.write(RESPSerializer.nullArray());
            else
                clientOutput.write(RESPSerializer.array(result));

            clientOutput.flush();
        } catch (InterruptedException exception) {
            try {
                clientOutput.write(RESPSerializer.error("Operation Interrupted"));
                clientOutput.flush();
                return;
            } catch (IOException exception2) {
                throw new RuntimeException(exception2);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
