package rdb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for encoding strings in RDB format.
 * 
 * Strings in RDB are encoded as:
 * [length][bytes]
 * 
 * Where length is encoded using RDBLengthEncoding's variable-length format.
 */
public class RDBStringEncoder {
    
    /**
     * Encodes a string and writes it to the output stream.
     * Format: [length][UTF-8 bytes]
     * 
     * @param value The string to encode
     * @param out The output stream to write to
     * @throws IOException If an I/O error occurs
     */
    public static void encodeString(String value, OutputStream out) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        
        // Convert string to UTF-8 bytes
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        
        // Write length using variable-length encoding
        RDBLengthEncoding.encodeLength(bytes.length, out);
        
        // Write the actual bytes
        out.write(bytes);
    }
    
    /**
     * Encodes a string and returns it as a byte array.
     * 
     * @param value The string to encode
     * @return Byte array containing the encoded string
     */
    public static byte[] encodeString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            encodeString(value, out);
            return out.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream doesn't throw IOException in practice
            throw new RuntimeException("Unexpected IOException with ByteArrayOutputStream", e);
        }
    }
    
    /**
     * Calculates the encoded size of a string (length encoding + UTF-8 bytes).
     * 
     * @param value The string to calculate size for
     * @return Number of bytes needed to encode this string
     */
    public static int getEncodedSize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int lengthEncodingSize = RDBLengthEncoding.getEncodedSize(bytes.length);
        return lengthEncodingSize + bytes.length;
    }
    
    // Private constructor to prevent instantiation
    private RDBStringEncoder() {
        throw new AssertionError("RDBStringEncoder utility class cannot be instantiated");
    }
}
