package rdb;

/**
 * Constants for RDB (Redis Database) file format.
 * 
 * RDB File Structure:
 * [MAGIC_STRING][VERSION][DATABASE_SECTION][EOF_OPCODE][CHECKSUM]
 */
public class RDBConstants {
    
    // ===== File Header =====
    /**
     * Magic string that identifies this as a Redis RDB file: "REDIS"
     */
    public static final String MAGIC_STRING = "REDIS";
    
    /**
     * RDB version number (as 4-byte ASCII string, e.g., "0009" for version 9)
     */
    public static final String VERSION = "0009";
    
    
    // ===== Op Codes =====
    /**
     * 0xFF - Marks the end of the RDB file
     */
    public static final int EOF_OPCODE = 0xFF;
    
    /**
     * 0xFE - Indicates database selector (followed by database number)
     */
    public static final int SELECTDB_OPCODE = 0xFE;
    
    /**
     * 0xFB - Indicates resize DB hint (hash table size info)
     */
    public static final int RESIZEDB_OPCODE = 0xFB;
    
    /**
     * 0xFD - Expiry time in seconds (4 bytes, Unix timestamp)
     */
    public static final int EXPIRETIME_OPCODE = 0xFD;
    
    /**
     * 0xFC - Expiry time in milliseconds (8 bytes, Unix timestamp)
     */
    public static final int EXPIRETIMEMS_OPCODE = 0xFC;
    
    /**
     * 0xFA - Auxiliary field (metadata: redis-ver, redis-bits, etc.)
     */
    public static final int AUX_OPCODE = 0xFA;
    
    
    // ===== Value Type Codes =====
    /**
     * 0 - String encoding
     */
    public static final int TYPE_STRING = 0;
    
    /**
     * 1 - List encoding
     */
    public static final int TYPE_LIST = 1;
    
    /**
     * 2 - Set encoding
     */
    public static final int TYPE_SET = 2;
    
    /**
     * 3 - Sorted set encoding
     */
    public static final int TYPE_ZSET = 3;
    
    /**
     * 4 - Hash encoding
     */
    public static final int TYPE_HASH = 4;
    
    /**
     * 9 - Zipmap encoding (deprecated)
     */
    public static final int TYPE_ZIPMAP = 9;
    
    /**
     * 10 - Ziplist encoding
     */
    public static final int TYPE_ZIPLIST = 10;
    
    /**
     * 11 - Intset encoding
     */
    public static final int TYPE_INTSET = 11;
    
    /**
     * 12 - Sorted set in ziplist encoding
     */
    public static final int TYPE_ZSET_ZIPLIST = 12;
    
    /**
     * 13 - Hash in ziplist encoding
     */
    public static final int TYPE_HASH_ZIPLIST = 13;
    
    /**
     * 14 - List in quicklist encoding (Redis 3.2+)
     */
    public static final int TYPE_LIST_QUICKLIST = 14;
    
    /**
     * 15 - Stream encoding (Redis 5.0+)
     */
    public static final int TYPE_STREAM = 15;
    
    
    // ===== Length Encoding =====
    /**
     * Length encoding for 6-bit length (0-63 bytes)
     * Format: 00xxxxxx
     */
    public static final int LENGTH_6BIT = 0b00000000;
    
    /**
     * Length encoding for 14-bit length (0-16383 bytes)
     * Format: 01xxxxxx xxxxxxxx
     */
    public static final int LENGTH_14BIT = 0b01000000;
    
    /**
     * Length encoding for 32-bit length
     * Format: 10000000 [4 bytes big-endian]
     */
    public static final int LENGTH_32BIT = 0b10000000;
    
    /**
     * Special encoding indicator
     * Format: 11xxxxxx
     */
    public static final int LENGTH_SPECIAL = 0b11000000;
    
    
    // ===== String Encoding Types (used with LENGTH_SPECIAL) =====
    /**
     * 8-bit signed integer
     */
    public static final int ENCODING_INT8 = 0;
    
    /**
     * 16-bit signed integer
     */
    public static final int ENCODING_INT16 = 1;
    
    /**
     * 32-bit signed integer
     */
    public static final int ENCODING_INT32 = 2;
    
    /**
     * LZF compressed string
     */
    public static final int ENCODING_LZF = 3;
    
    
    // ===== Checksum =====
    /**
     * Size of CRC64 checksum in bytes
     */
    public static final int CHECKSUM_SIZE = 8;
    
    
    // ===== Default Values =====
    /**
     * Default database number (DB 0)
     */
    public static final int DEFAULT_DATABASE = 0;
    
    
    // Private constructor to prevent instantiation
    private RDBConstants() {
        throw new AssertionError("RDBConstants class cannot be instantiated");
    }
}
