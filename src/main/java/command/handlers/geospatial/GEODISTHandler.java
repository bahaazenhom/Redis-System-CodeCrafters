package command.handlers.geospatial;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;
import util.GeospatialDecoding;

public class GEODISTHandler implements CommandStrategy {
    private final DataStore dataStore;
    
    // Earth's radius in meters
    private static final double EARTH_RADIUS_METERS = 6372797.560856;

    public GEODISTHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            String member1 = arguments.get(1);
            String member2 = arguments.get(2);
            String unit = arguments.size() > 3 ? arguments.get(3).toLowerCase() : "m";
            
            double member1Score = dataStore.zscore(key, member1);
            double member2Score = dataStore.zscore(key, member2);
            
            if (member1Score == -1 || member2Score == -1) {
                clientOutput.write(RESPSerializer.nullBulkString());
            } else {
                // Decode geohashes to coordinates
                double[] coords1 = GeospatialDecoding.decode((long) member1Score);
                double[] coords2 = GeospatialDecoding.decode((long) member2Score);
                
                // Calculate distance using Haversine formula
                double distance = calculateDistance(coords1[0], coords1[1], coords2[0], coords2[1]);
                
                // Convert to requested unit
                distance = convertUnit(distance, unit);
                
                clientOutput.write(RESPSerializer.bulkString(String.valueOf(distance)));
            }
            clientOutput.flush();
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEODIST command.");
        }
    }
    
    /**
     * Calculate distance between two points using the Haversine formula
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert to radians
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lon1Rad = Math.toRadians(lon1);
        double lon2Rad = Math.toRadians(lon2);
        
        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Convert distance from meters to the requested unit
     */
    private double convertUnit(double distanceInMeters, String unit) {
        return switch (unit) {
            case "m" -> distanceInMeters;
            case "km" -> distanceInMeters / 1000.0;
            case "mi" -> distanceInMeters / 1609.34;
            case "ft" -> distanceInMeters * 3.28084;
            default -> distanceInMeters;
        };
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 3 || arguments.size() > 4) {
            throw new IllegalArgumentException("GEODIST requires 3 or 4 arguments: key, member1, member2, [unit].");
        }
        if (arguments.size() == 4) {
            String unit = arguments.get(3).toLowerCase();
            if (!unit.equals("m") && !unit.equals("km") && !unit.equals("mi") && !unit.equals("ft")) {
                throw new IllegalArgumentException("Invalid unit. Supported units: m, km, mi, ft.");
            }
        }
    }

}
