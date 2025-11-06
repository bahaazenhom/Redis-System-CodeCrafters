package rdb;

import rdb.util.RDBLengthEncoding;
import rdb.util.RDBStringDecoder;
import storage.core.DataType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Low-level reader for RDB file format.
 * Handles reading and parsing individual RDB components.
 */
public class RDBReader {
    
    private final InputStream in;
    private Long currentExpiryTime = null; // Tracks expiry for the next key-value pair
    
    public RDBReader(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        this.in = in;
    }
    
    /**
     * Reads and validates the RDB file header.
     * Expected format: "REDIS" + version (e.g., "0009")
     * 
     * @return The RDB version string (e.g., "0009")
     * @throws IOException If an I/O error occurs
     * @throws RDBException If the header is invalid
     */
    public String readHeader() throws IOException, RDBException {
        // Read magic string "REDIS" (5 bytes)
        byte[] magicBytes = new byte[RDBConstants.MAGIC_STRING.length()];
        int bytesRead = in.read(magicBytes);
        if (bytesRead != magicBytes.length) {
            throw new RDBException("Invalid RDB file: magic string not found");
        }
        
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);
        if (!magic.equals(RDBConstants.MAGIC_STRING)) {
            throw new RDBException("Invalid RDB file: expected '" + RDBConstants.MAGIC_STRING + "', got '" + magic + "'");
        }
        
        // Read version (4 bytes)
        byte[] versionBytes = new byte[RDBConstants.VERSION.length()];
        bytesRead = in.read(versionBytes);
        if (bytesRead != versionBytes.length) {
            throw new RDBException("Invalid RDB file: version not found");
        }
        
        String version = new String(versionBytes, StandardCharsets.US_ASCII);
        return version;
    }
    
    /**
     * Reads the next opcode from the stream.
     * 
     * @return The opcode byte, or -1 if end of stream
     * @throws IOException If an I/O error occurs
     */
    public int readOpcode() throws IOException {
        return in.read();
    }
    
    /**
     * Reads a SELECTDB opcode payload (database number).
     * 
     * @return The database number
     * @throws IOException If an I/O error occurs
     */
    public int readSelectDB() throws IOException {
        return (int) RDBLengthEncoding.decodeLength(in).getValue();
    }
    
    /**
     * Reads an expiry time in milliseconds (8 bytes, little-endian).
     * Stores it for the next key-value pair.
     * 
     * @throws IOException If an I/O error occurs
     */
    public void readExpiryTimeMs() throws IOException {
        // Read 8 bytes in little-endian format
        long timestamp = 0;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream while reading expiry time");
            }
            timestamp |= ((long) b) << (i * 8);
        }
        this.currentExpiryTime = timestamp;
    }
    
    /**
     * Reads an expiry time in seconds (4 bytes, little-endian).
     * Converts to milliseconds and stores it for the next key-value pair.
     * 
     * @throws IOException If an I/O error occurs
     */
    public void readExpiryTimeSec() throws IOException {
        // Read 4 bytes in little-endian format
        long timestamp = 0;
        for (int i = 0; i < 4; i++) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream while reading expiry time");
            }
            timestamp |= ((long) b) << (i * 8);
        }
        // Convert seconds to milliseconds
        this.currentExpiryTime = timestamp * 1000;
    }
    
    /**
     * Reads an AUX field (auxiliary metadata).
     * Format: key (string) + value (string)
     * These fields contain metadata like redis-ver, redis-bits, etc.
     * We read and discard them for now.
     * 
     * @throws IOException If an I/O error occurs
     */
    @SuppressWarnings("unused")
    public void readAuxField() throws IOException {
        // Read key (string)
        String key = RDBStringDecoder.decodeString(in);
        // Read value (string)
        String value = RDBStringDecoder.decodeString(in);
        // Currently just discard - could log if needed
        // System.out.println("AUX field: " + key + " = " + value);
    }
    
    /**
     * Reads a key-value pair from the RDB file.
     * 
     * @param valueType The type of the value (read from previous opcode)
     * @return RDBKeyValue containing the key, value, and optional expiry
     * @throws IOException If an I/O error occurs
     * @throws RDBException If the format is invalid
     */
    public RDBKeyValue readKeyValue(int valueType) throws IOException, RDBException {
        // Read key (always a string)
        String key = RDBStringDecoder.decodeString(in);
        
        // Read value based on type
        Object value = readValue(valueType);
        
        // Get and clear expiry time
        Long expiryTime = this.currentExpiryTime;
        this.currentExpiryTime = null;
        
        return new RDBKeyValue(key, value, getDataType(valueType), expiryTime);
    }
    
    /**
     * Reads a value based on its type.
     * 
     * @param valueType The RDB value type constant
     * @return The decoded value
     * @throws IOException If an I/O error occurs
     * @throws RDBException If the type is unsupported
     */
    private Object readValue(int valueType) throws IOException, RDBException {
        switch (valueType) {
            case RDBConstants.TYPE_STRING:
                return RDBStringDecoder.decodeString(in);
                
            case RDBConstants.TYPE_LIST:
            case RDBConstants.TYPE_SET:
            case RDBConstants.TYPE_ZSET:
            case RDBConstants.TYPE_HASH:
            case RDBConstants.TYPE_STREAM:
                throw new UnsupportedOperationException("Type " + valueType + " not yet supported");
                
            default:
                throw new RDBException("Unknown value type: " + valueType);
        }
    }
    
    /**
     * Converts RDB value type constant to DataType enum.
     * 
     * @param valueType The RDB value type constant
     * @return The corresponding DataType
     */
    private DataType getDataType(int valueType) {
        switch (valueType) {
            case RDBConstants.TYPE_STRING:
                return DataType.STRING;
            case RDBConstants.TYPE_LIST:
            case RDBConstants.TYPE_LIST_QUICKLIST:
                return DataType.LIST;
            case RDBConstants.TYPE_STREAM:
                return DataType.STREAM;
            default:
                return DataType.STRING; // Default to string
        }
    }
    
    /**
     * Reads and validates the checksum (8 bytes).
     * Currently just reads and discards the bytes.
     * 
     * @throws IOException If an I/O error occurs
     */
    public void readChecksum() throws IOException {
        byte[] checksum = new byte[RDBConstants.CHECKSUM_SIZE];
        int bytesRead = in.read(checksum);
        if (bytesRead != RDBConstants.CHECKSUM_SIZE) {
            throw new IOException("Failed to read checksum");
        }
        // TODO: Validate checksum
    }
    
    /**
     * Container class for a key-value pair read from RDB file.
     */
    public static class RDBKeyValue {
        private final String key;
        private final Object value;
        private final DataType type;
        private final Long expiryTime;
        
        public RDBKeyValue(String key, Object value, DataType type, Long expiryTime) {
            this.key = key;
            this.value = value;
            this.type = type;
            this.expiryTime = expiryTime;
        }
        
        public String getKey() {
            return key;
        }
        
        public Object getValue() {
            return value;
        }
        
        public DataType getType() {
            return type;
        }
        
        public Long getExpiryTime() {
            return expiryTime;
        }
        
        public boolean hasExpiry() {
            return expiryTime != null;
        }
    }
}
