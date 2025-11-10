package util;

public class GeospatialDecoding {
    private static final double MIN_LATITUDE = -85.05112878;
    private static final double MAX_LATITUDE = 85.05112878;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
    private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

    static class Coordinates {
        double latitude;
        double longitude;

        Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static long compactInt64(long v) {
        v = v & 0x5555555555555555L;
        v = (v | (v >> 1)) & 0x3333333333333333L;
        v = (v | (v >> 2)) & 0x0F0F0F0F0F0F0F0FL;
        v = (v | (v >> 4)) & 0x00FF00FF00FF00FFL;
        v = (v | (v >> 8)) & 0x0000FFFF0000FFFFL;
        v = (v | (v >> 16)) & 0x00000000FFFFFFFFL;
        return v;
    }

    private static double[] convertGridNumbersToCoordinates(long gridLatitudeNumber, long gridLongitudeNumber) {
        // Use double precision throughout to avoid truncation errors
        double normalizedLatitude = gridLatitudeNumber / Math.pow(2, 26);
        double normalizedLongitude = gridLongitudeNumber / Math.pow(2, 26);

        // Convert normalized values back to actual coordinates
        double latitude = MIN_LATITUDE + LATITUDE_RANGE * normalizedLatitude;
        double longitude = MIN_LONGITUDE + LONGITUDE_RANGE * normalizedLongitude;

        double[] coords = new double[2];
        coords[0] = latitude;
        coords[1] = longitude;
        return coords;
    }

    public static double[] decode(long geoCode) {
        // Align bits of both latitude and longitude to take even-numbered position
        long y = geoCode >> 1;
        long x = geoCode;

        // Compact bits back to full precision longs (no truncation to int)
        long gridLatitudeNumber = compactInt64(x);
        long gridLongitudeNumber = compactInt64(y);

        return convertGridNumbersToCoordinates(gridLatitudeNumber, gridLongitudeNumber);
    }

}