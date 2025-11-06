package rdb.util;

import rdb.RDBConstants;
import rdb.util.RDBLengthEncoding.LengthEncodingResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for decoding strings from RDB format.
 * 
 * Strings in RDB are encoded as:
 * [length][bytes]
 * 
 * Where length is encoded using RDBLengthEncoding's variable-length format.
 * Special encodings (integer encoded strings) are also supported.
 */
public class RDBStringDecoder {
    
    /**
     * Decodes a string from the input stream.
     * Handles both regular strings and special integer encodings.
     * 
     * @param in The input stream to read from
     * @return The decoded string
     * @throws IOException If an I/O error occurs
     */
    public static String decodeString(InputStream in) throws IOException {
        LengthEncodingResult lengthResult = RDBLengthEncoding.decodeLength(in);
        
        // Check if this is a special encoding (integer encoded string)
        if (lengthResult.isSpecialEncoding()) {
            return decodeIntegerEncodedString(in, (int) lengthResult.getValue());
        }
        
        // Regular string encoding
        int length = (int) lengthResult.getValue();
        byte[] bytes = new byte[length];
        
        int bytesRead = in.read(bytes);
        if (bytesRead != length) {
            throw new IOException("Unexpected end of stream while reading string. Expected " + length + " bytes, got " + bytesRead);
        }
        
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Decodes an integer-encoded string.
     * Redis can encode small integers directly in the RDB file for efficiency.
     * 
     * @param in The input stream
     * @param encodingType The type of integer encoding (INT8, INT16, INT32)
     * @return The decoded string representation of the integer
     * @throws IOException If an I/O error occurs
     */
    private static String decodeIntegerEncodedString(InputStream in, int encodingType) throws IOException {
        switch (encodingType) {
            case RDBConstants.ENCODING_INT8:
                // Read 1 byte signed integer
                int byte1 = in.read();
                if (byte1 == -1) {
                    throw new IOException("Unexpected end of stream while reading INT8");
                }
                return String.valueOf((byte) byte1);
                
            case RDBConstants.ENCODING_INT16:
                // Read 2 bytes signed integer (little-endian)
                int low = in.read();
                int high = in.read();
                if (low == -1 || high == -1) {
                    throw new IOException("Unexpected end of stream while reading INT16");
                }
                short int16 = (short) (low | (high << 8));
                return String.valueOf(int16);
                
            case RDBConstants.ENCODING_INT32:
                // Read 4 bytes signed integer (little-endian)
                int b1 = in.read();
                int b2 = in.read();
                int b3 = in.read();
                int b4 = in.read();
                if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
                    throw new IOException("Unexpected end of stream while reading INT32");
                }
                int int32 = b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
                return String.valueOf(int32);
                
            case RDBConstants.ENCODING_LZF:
                throw new UnsupportedOperationException("LZF compressed strings not yet supported");
                
            default:
                throw new IOException("Unknown string encoding type: " + encodingType);
        }
    }
    
    // Private constructor to prevent instantiation
    private RDBStringDecoder() {
        throw new AssertionError("RDBStringDecoder utility class cannot be instantiated");
    }
}
