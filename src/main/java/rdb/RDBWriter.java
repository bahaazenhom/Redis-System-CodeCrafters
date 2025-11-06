package rdb;

import rdb.util.RDBLengthEncoding;
import rdb.util.RDBStringEncoder;
import storage.core.RedisValue;
import storage.types.StringValue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Low-level writer for RDB file format.
 * Handles writing individual RDB components (header, opcodes, key-value pairs, etc.)
 */
public class RDBWriter {
    
    private final OutputStream out;
    
    public RDBWriter(OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("OutputStream cannot be null");
        }
        this.out = out;
    }
    
    /**
     * Writes the RDB file header: "REDIS" + version "0009"
     * 
     * @throws IOException If an I/O error occurs
     */
    public void writeHeader() throws IOException {
        out.write(RDBConstants.MAGIC_STRING.getBytes(StandardCharsets.US_ASCII));
        out.write(RDBConstants.VERSION.getBytes(StandardCharsets.US_ASCII));
    }
    
    /**
     * Writes the SELECTDB opcode followed by the database number.
     * Format: 0xFE [db_number as length-encoded]
     * 
     * @param dbNumber The database number (typically 0)
     * @throws IOException If an I/O error occurs
     */
    public void writeSelectDB(int dbNumber) throws IOException {
        out.write(RDBConstants.SELECTDB_OPCODE);
        RDBLengthEncoding.encodeLength(dbNumber, out);
    }
    
    /**
     * Writes a key-value pair to the RDB file.
     * Handles expiry time if present.
     * Currently only supports STRING type values.
     * 
     * Format (with expiry):
     * [0xFC][8-byte millisecond timestamp][value_type][key][value]
     * 
     * Format (no expiry):
     * [value_type][key][value]
     * 
     * @param key The key (always a string)
     * @param value The RedisValue (currently only StringValue supported)
     * @throws IOException If an I/O error occurs
     */
    public void writeKeyValuePair(String key, RedisValue value) throws IOException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
        
        // Write expiry time if present (0xFC + 8 bytes)
        if (value.hasExpiry()) {
            out.write(RDBConstants.EXPIRETIMEMS_OPCODE);
            long expiryTimeMs = value.getExpiryTime();
            
            // Write 8 bytes in little-endian format (Redis uses little-endian for timestamps)
            out.write((int) (expiryTimeMs & 0xFF));
            out.write((int) ((expiryTimeMs >> 8) & 0xFF));
            out.write((int) ((expiryTimeMs >> 16) & 0xFF));
            out.write((int) ((expiryTimeMs >> 24) & 0xFF));
            out.write((int) ((expiryTimeMs >> 32) & 0xFF));
            out.write((int) ((expiryTimeMs >> 40) & 0xFF));
            out.write((int) ((expiryTimeMs >> 48) & 0xFF));
            out.write((int) ((expiryTimeMs >> 56) & 0xFF));
        }
        
        // Write value type
        writeValueType(value);
        
        // Write key (always a string)
        RDBStringEncoder.encodeString(key, out);
        
        // Write value (based on type)
        writeValue(value);
    }
    
    /**
     * Writes the value type byte.
     * 
     * @param value The RedisValue
     * @throws IOException If an I/O error occurs
     */
    private void writeValueType(RedisValue value) throws IOException {
        switch (value.getType()) {
            case STRING:
                out.write(RDBConstants.TYPE_STRING);
                break;
            case LIST:
                out.write(RDBConstants.TYPE_LIST);
                break;
            case STREAM:
                out.write(RDBConstants.TYPE_STREAM);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported value type: " + value.getType());
        }
    }
    
    /**
     * Writes the value data based on its type.
     * Currently only supports STRING type.
     * 
     * @param value The RedisValue
     * @throws IOException If an I/O error occurs
     */
    private void writeValue(RedisValue value) throws IOException {
        switch (value.getType()) {
            case STRING:
                StringValue stringValue = (StringValue) value;
                RDBStringEncoder.encodeString(stringValue.getString(), out);
                break;
            case LIST:
                throw new UnsupportedOperationException("LIST encoding not yet implemented");
            case STREAM:
                throw new UnsupportedOperationException("STREAM encoding not yet implemented");
            default:
                throw new UnsupportedOperationException("Unsupported value type: " + value.getType());
        }
    }
    
    /**
     * Writes the EOF opcode (0xFF).
     * 
     * @throws IOException If an I/O error occurs
     */
    public void writeEOF() throws IOException {
        out.write(RDBConstants.EOF_OPCODE);
    }
    
    /**
     * Writes the CRC64 checksum (8 bytes).
     * For now, writes zeros as a placeholder.
     * 
     * TODO: Implement actual CRC64 checksum calculation
     * 
     * @throws IOException If an I/O error occurs
     */
    public void writeChecksum() throws IOException {
        // Write 8 bytes of zeros as placeholder
        for (int i = 0; i < RDBConstants.CHECKSUM_SIZE; i++) {
            out.write(0);
        }
    }
    
    /**
     * Flushes the output stream.
     * 
     * @throws IOException If an I/O error occurs
     */
    public void flush() throws IOException {
        out.flush();
    }
}
