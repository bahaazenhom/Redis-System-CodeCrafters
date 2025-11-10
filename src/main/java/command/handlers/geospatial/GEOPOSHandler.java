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
            List<String> members = arguments.subList(1, arguments.size());
            List<List<String>> results = new java.util.ArrayList<>();
            for (String member : members) {

                long geoScore = (long) dataStore.zscore(key, member);
                if (geoScore == -1) {
                    results.add(null);
                    continue;
                }

                double[] coordinates = GeospatialDecoding.decode(geoScore);
                // Redis GEOPOS returns [longitude, latitude], but decode() returns [latitude, longitude]
                List<String> coordList = Arrays.asList(
                        String.valueOf(coordinates[1]), // longitude first
                        String.valueOf(coordinates[0])); // latitude second

                results.add(coordList);
            }

            // Send response back to client
            clientOutput.write(RESPSerializer.arrayOfArrays(results));
            clientOutput.flush();
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEOPOS command.");
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("GEOPOS requires at least 2 arguments: key and one or more members.");
        }
    }

}
