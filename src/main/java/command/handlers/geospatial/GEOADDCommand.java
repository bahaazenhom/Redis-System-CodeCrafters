package command.handlers.geospatial;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class GEOADDCommand implements CommandStrategy {
    private final DataStore dataStore;

    public GEOADDCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {

            String key = arguments.get(0);
            int numAdded = 0;

            // Process each (longitude, latitude, member) triplet
            for (int i = 1; i < arguments.size(); i += 3) {
                String longitudeStr = arguments.get(i);
                String latitudeStr = arguments.get(i + 1);
                String member = arguments.get(i + 2);

                double longitude = Double.parseDouble(longitudeStr);
                double latitude = Double.parseDouble(latitudeStr);

                numAdded++;
            }

            // Send response back to client
            clientOutput.write(RESPSerializer.integer(numAdded));
            clientOutput.flush();

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Longitude and latitude must be valid floating point numbers.");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEOADD command.");
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 4 || (arguments.size() - 1) % 3 != 0) {
            throw new IllegalArgumentException(
                    "GEOADD requires at least 4 arguments and the number of arguments after the key must be a multiple of 3.");
        }
    }

}
