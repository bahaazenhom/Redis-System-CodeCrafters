package rdb.util;

import rdb.RDBConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Utility class for encoding and decoding length values in RDB format.
 * 
 * Redis uses a variable-length encoding scheme to save space:
 * - 6-bit:  Values 0-63 encoded as single byte (00xxxxxx)
 * - 14-bit: Values 0-16383 encoded as 2 bytes (01xxxxxx xxxxxxxx)
 * - 32-bit: Larger values encoded as 5 bytes (10000000 + 4-byte big-endian)
 * - Special: Integer encodings (11xxxxxx) for optimized string storage
 */
public class RDBLengthEncoding {
    
    /**
     * Maximum value that can be encoded in 6 bits
     */
    private static final int MAX_6BIT = 63;
    
    /**
     * Maximum value that can be encoded in 14 bits
     */
    private static final int MAX_14BIT = 16383;
    
    /**
     * Mask to extract the encoding type from the first byte (top 2 bits)
     */
    private static final int ENCODING_TYPE_MASK = 0b11000000;
    //                                              01000000
    
    /**
     * Mask to extract the 6-bit value
     */
    private static final int VALUE_6BIT_MASK = 0b00111111;
    
    /**
     * Mask to extract the top 6 bits of a 14-bit value
     */
    private static final int VALUE_14BIT_HIGH_MASK = 0b00111111;
    
    
    // ============================================
    // ENCODING METHODS
    // ============================================
    
    /**
     * Encodes a length value and writes it to the output stream.
     * 
     * @param length The length value to encode (must be non-negative)
     * @param out The output stream to write to
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException If length is negative
     */
    public static void encodeLength(long length, OutputStream out) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        if (length <= MAX_6BIT) {
            // 6-bit encoding: 00xxxxxx
            out.write((int) (RDBConstants.LENGTH_6BIT | length));
            
        } else if (length <= MAX_14BIT) {
            // 14-bit encoding: 01xxxxxx xxxxxxxx
            int highByte = (int) ((length >> 8) & VALUE_14BIT_HIGH_MASK);
            int lowByte = (int) (length & 0xFF);
            
            out.write(RDBConstants.LENGTH_14BIT | highByte);
            out.write(lowByte);
            
        } else {
            // 32-bit encoding: 10000000 [4 bytes big-endian]
            out.write(RDBConstants.LENGTH_32BIT);
            
            // Write 4 bytes in big-endian order
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt((int) length);
            out.write(buffer.array());
        }
    }
    
    /**
     * Encodes a length value and returns it as a byte array.
     * 
     * @param length The length value to encode (must be non-negative)
     * @return Byte array containing the encoded length
     * @throws IllegalArgumentException If length is negative
     */
    public static byte[] encodeLength(long length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        if (length <= MAX_6BIT) {
            // 6-bit: 1 byte
            return new byte[] { (byte) (RDBConstants.LENGTH_6BIT | length) };
            
        } else if (length <= MAX_14BIT) {
            // 14-bit: 2 bytes
            int highByte = (int) ((length >> 8) & VALUE_14BIT_HIGH_MASK);
            int lowByte = (int) (length & 0xFF);
            
            return new byte[] {
                (byte) (RDBConstants.LENGTH_14BIT | highByte),
                (byte) lowByte
            };
            
        } else {
            // 32-bit: 5 bytes (marker + 4 bytes)
            ByteBuffer buffer = ByteBuffer.allocate(5);
            buffer.put((byte) RDBConstants.LENGTH_32BIT);
            buffer.putInt((int) length);
            return buffer.array();
        }
    }
    
    
    // ============================================
    // DECODING METHODS
    // ============================================
    
    /**
     * Decodes a length value from an input stream.
     * 
     * @param in The input stream to read from
     * @return LengthEncodingResult containing the decoded length and encoding type
     * @throws IOException If an I/O error occurs or stream ends unexpectedly
     */
    public static LengthEncodingResult decodeLength(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1) {
            throw new IOException("Unexpected end of stream while reading length encoding");
        }
        
        int encodingType = firstByte & ENCODING_TYPE_MASK;
        
        switch (encodingType) {
            case RDBConstants.LENGTH_6BIT:
                // 6-bit: value is in the lower 6 bits
                long value6 = firstByte & VALUE_6BIT_MASK;
                return new LengthEncodingResult(value6, EncodingType.LENGTH_6BIT);
                
            case RDBConstants.LENGTH_14BIT:
                // 14-bit: read next byte and combine
                int secondByte = in.read();
                if (secondByte == -1) {
                    throw new IOException("Unexpected end of stream while reading 14-bit length");
                }
                
                long highBits = (firstByte & VALUE_14BIT_HIGH_MASK) << 8;
                long value14 = highBits | secondByte;
                return new LengthEncodingResult(value14, EncodingType.LENGTH_14BIT);
                
            case RDBConstants.LENGTH_32BIT:
                // 32-bit: read 4 bytes in big-endian order
                byte[] bytes = new byte[4];
                int bytesRead = in.read(bytes);
                if (bytesRead != 4) {   
                    throw new IOException("Unexpected end of stream while reading 32-bit length");
                }
                
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                long value32 = buffer.getInt() & 0xFFFFFFFFL; // Convert to unsigned
                return new LengthEncodingResult(value32, EncodingType.LENGTH_32BIT);
                
            case RDBConstants.LENGTH_SPECIAL:
                // Special encoding for integers
                int specialType = firstByte & VALUE_6BIT_MASK;
                return new LengthEncodingResult(specialType, EncodingType.SPECIAL_ENCODING);
                
            default:
                throw new IOException("Invalid length encoding type: " + encodingType);
        }
    }
    
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Determines the encoding type that would be used for a given length.
     * Useful for calculating size before encoding.
     * 
     * @param length The length value
     * @return The encoding type that would be used
     */
    public static EncodingType getEncodingType(long length) {
        if (length <= MAX_6BIT) {
            return EncodingType.LENGTH_6BIT;
        } else if (length <= MAX_14BIT) {
            return EncodingType.LENGTH_14BIT;
        } else {
            return EncodingType.LENGTH_32BIT;
        }
    }
    
    /**
     * Calculates how many bytes are needed to encode a given length.
     * 
     * @param length The length value
     * @return Number of bytes needed (1, 2, or 5)
     */
    public static int getEncodedSize(long length) {
        if (length <= MAX_6BIT) {
            return 1;
        } else if (length <= MAX_14BIT) {
            return 2;
        } else {
            return 5;
        }
    }
    
    
    // ============================================
    // NESTED CLASSES
    // ============================================
    
    /**
     * Result of decoding a length value, containing both the value and its encoding type.
     */
    public static class LengthEncodingResult {
        private final long value;
        private final EncodingType type;
        
        public LengthEncodingResult(long value, EncodingType type) {
            this.value = value;
            this.type = type;
        }
        
        public long getValue() {
            return value;
        }
        
        public EncodingType getType() {
            return type;
        }
        
        public boolean isSpecialEncoding() {
            return type == EncodingType.SPECIAL_ENCODING;
        }
        
        @Override
        public String toString() {
            return "LengthEncodingResult{value=" + value + ", type=" + type + "}";
        }
    }
    
    /**
     * Enum representing the different types of length encoding.
     */
    public enum EncodingType {
        LENGTH_6BIT,
        LENGTH_14BIT,
        LENGTH_32BIT,
        SPECIAL_ENCODING
    }
    
    
    // Private constructor to prevent instantiation
    private RDBLengthEncoding() {
        throw new AssertionError("RDBLengthEncoding utility class cannot be instantiated");
    }
}
