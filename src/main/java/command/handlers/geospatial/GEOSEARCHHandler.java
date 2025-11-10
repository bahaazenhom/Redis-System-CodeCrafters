package command.handlers.geospatial;

import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;
import util.GeospatialDecoding;

public class GEOSEARCHHandler implements CommandStrategy {
    private final DataStore dataStore;
    private static final double EARTH_RADIUS_METERS = 6372797.560856;

    public GEOSEARCHHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);

            double centerLon = Double.parseDouble(arguments.get(2));
            double centerLat = Double.parseDouble(arguments.get(3));
      
            double radius = Double.parseDouble(arguments.get(5));
            String unit = arguments.size() > 6 ? arguments.get(6).toLowerCase() : "m";
            
            // Convert radius to meters
            double radiusInMeters = convertToMeters(radius, unit);
            
            // Get all members in the key
            List<String> allMembers = dataStore.zrange(key, 0, -1);
            
            List<String> matchingMembers = new ArrayList<>();
            
            // Check each member
            for (String member : allMembers) {
                double score = dataStore.zscore(key, member);
                if (score == -1) continue;
                
                // Decode geohash to get coordinates
                double[] coords = GeospatialDecoding.decode((long) score);
                double memberLat = coords[0];
                double memberLon = coords[1];
                
                // Calculate distance
                double distance = calculateDistance(centerLat, centerLon, memberLat, memberLon);
                
                // Check if within radius
                if (distance <= radiusInMeters) {
                    matchingMembers.add(member);
                }
            }
            
            // Send response as RESP array
            clientOutput.write(RESPSerializer.array(matchingMembers));
            clientOutput.flush();
            
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while processing GEOSEARCH command: " + e.getMessage());
        }
    }
    
    /**
     * Calculate distance between two points using the Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lon1Rad = Math.toRadians(lon1);
        double lon2Rad = Math.toRadians(lon2);
        
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Convert distance from the given unit to meters
     */
    private double convertToMeters(double distance, String unit) {
        return switch (unit) {
            case "m" -> distance;
            case "km" -> distance * 1000.0;
            case "mi" -> distance * 1609.34;
            case "ft" -> distance / 3.28084;
            default -> distance;
        };
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 7) {
            throw new IllegalArgumentException(
                    "GEOSEARCH requires at least 7 arguments: key FROMLONLAT longitude latitude BYRADIUS radius [unit]");
        }
        
        if (!arguments.get(1).equalsIgnoreCase("FROMLONLAT")) {
            throw new IllegalArgumentException("Second argument must be FROMLONLAT");
        }
        
        if (!arguments.get(4).equalsIgnoreCase("BYRADIUS")) {
            throw new IllegalArgumentException("Fifth argument must be BYRADIUS");
        }
        
        try {
            Double.parseDouble(arguments.get(2)); // longitude
            Double.parseDouble(arguments.get(3)); // latitude
            Double.parseDouble(arguments.get(5)); // radius
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Longitude, latitude, and radius must be valid numbers");
        }
        
        if (arguments.size() > 6) {
            String unit = arguments.get(6).toLowerCase();
            if (!unit.equals("m") && !unit.equals("km") && !unit.equals("mi") && !unit.equals("ft")) {
                throw new IllegalArgumentException("Invalid unit. Supported units: m, km, mi, ft");
            }
        }
    }

}
