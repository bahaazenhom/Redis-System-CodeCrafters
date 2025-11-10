package command.handlers.geospatial;

import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import domain.values.Member;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class GEOADDHandler implements CommandStrategy {
    private final DataStore dataStore;

    public GEOADDHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {

            String key = arguments.get(0);
            int numAdded = 0;

            List<Member> members = new ArrayList<>();
            // Process each (longitude, latitude, member) triplet
            for (int i = 1; i < arguments.size(); i += 3) {
                String longitudeStr = arguments.get(i);
                String latitudeStr = arguments.get(i + 1);
                String member = arguments.get(i + 2);

                double longitude = Double.parseDouble(longitudeStr);
                double latitude = Double.parseDouble(latitudeStr);

                Member geoMember = new Member(member, longitude, latitude);
                members.add(geoMember);
            }
            numAdded = dataStore.zadd(key, members);

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
        for (int i = 1; i < arguments.size(); i += 3) {
            try {
                double longitude = Double.parseDouble(arguments.get(i));
                double latitude = Double.parseDouble(arguments.get(i + 1));
                if (longitude < -180.0 || longitude > 180.0) {
                    throw new IllegalArgumentException("invalid longitude,latitude pair " + longitude + "," + latitude);
                }
                if (latitude < -85.05112878 || latitude > 85.05112878) {
                    throw new IllegalArgumentException("invalid longitude,latitude pair " + longitude + "," + latitude);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Longitude and latitude must be valid floating point numbers.");
            }
        }
    }

}
