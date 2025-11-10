package util;

public class GeospatialEncoding {
    private static final double MIN_LATITUDE = -85.05112878;
    private static final double MAX_LATITUDE = 85.05112878;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
    private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

    private static long spreadInt32ToInt64(int v) {
        long result = v & 0xFFFFFFFFL;
        result = (result | (result << 16)) & 0x0000FFFF0000FFFFL;
        result = (result | (result << 8)) & 0x00FF00FF00FF00FFL;
        result = (result | (result << 4)) & 0x0F0F0F0F0F0F0F0FL;
        result = (result | (result << 2)) & 0x3333333333333333L;
        result = (result | (result << 1)) & 0x5555555555555555L;
        return result;
    }

    private static long interleave(int x, int y) {
        long xSpread = spreadInt32ToInt64(x);
        long ySpread = spreadInt32ToInt64(y);
        long yShifted = ySpread << 1;
        return xSpread | yShifted;
    }

    public static long encode(double latitude, double longitude) {
        // Normalize to the range 0-2^26
        double normalizedLatitude = Math.pow(2, 26) * (latitude - MIN_LATITUDE) / LATITUDE_RANGE;
        double normalizedLongitude = Math.pow(2, 26) * (longitude - MIN_LONGITUDE) / LONGITUDE_RANGE;

        // Truncate to integers
        int latInt = (int) normalizedLatitude;
        int lonInt = (int) normalizedLongitude;

        return interleave(latInt, lonInt);
    }

}