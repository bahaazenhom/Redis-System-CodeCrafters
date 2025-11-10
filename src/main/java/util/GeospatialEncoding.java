package util;

public class GeospatialEncoding {
    private static final double MIN_LATITUDE = -85.05112878;
    private static final double MAX_LATITUDE = 85.05112878;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
    private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

    private static long spreadToInt64(long v) {
        v = v & 0xFFFFFFFFL;
        v = (v | (v << 16)) & 0x0000FFFF0000FFFFL;
        v = (v | (v << 8)) & 0x00FF00FF00FF00FFL;
        v = (v | (v << 4)) & 0x0F0F0F0F0F0F0F0FL;
        v = (v | (v << 2)) & 0x3333333333333333L;
        v = (v | (v << 1)) & 0x5555555555555555L;
        return v;
    }

    private static long interleave(long x, long y) {
        long xSpread = spreadToInt64(x);
        long ySpread = spreadToInt64(y);
        long yShifted = ySpread << 1;
        return xSpread | yShifted;
    }

    public static long encode(double latitude, double longitude) {
        // Normalize to the range 0-2^26 with full double precision
        double normalizedLatitude = Math.pow(2, 26) * (latitude - MIN_LATITUDE) / LATITUDE_RANGE;
        double normalizedLongitude = Math.pow(2, 26) * (longitude - MIN_LONGITUDE) / LONGITUDE_RANGE;

        // Convert to long for maximum precision (no truncation to int)
        long latLong = (long) normalizedLatitude;
        long lonLong = (long) normalizedLongitude;

        return interleave(latLong, lonLong);
    }

}