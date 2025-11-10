package command.handlers.geospatial;

import java.util.Arrays;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;
import util.GeospatialDecoding;

public class GEOPOSHandler implements CommandStrategy {
    private final DataStore dataStore;

    public GEOPOSHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String member = arguments.get(1);

            long geoScore = (long) dataStore.zscore(key, member);
            if (geoScore == -1) {
                clientOutput.write(RESPSerializer.nullArray());
                clientOutput.flush();
                return;
            }
            
            double[] coordinates = GeospatialDecoding.decode(geoScore);
            List<String> coordList = Arrays.asList(
                    String.valueOf(coordinates[0]),
                    String.valueOf(coordinates[1]));
            clientOutput.write(RESPSerializer.array(coordList));
            clientOutput.flush();
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEOPOS command.");
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("GEOPOS requires at least 2 arguments: key and one or more members.");
        }
    }

}
