package rdb;

import rdb.util.RDBLengthEncoding;
import storage.DataStore;
import domain.DataType;
import domain.RedisValue;
import domain.values.StringValue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * High-level manager for RDB persistence operations.
 * Orchestrates saving the entire database to an RDB file.
 */
public class RDBManager {
    
    private final DataStore dataStore;
    private final String rdbFilePath;
    
    /**
     * Creates an RDBManager with the specified data store and file path.
     * 
     * @param dataStore The data store to save
     * @param rdbFilePath The full path where the RDB file will be saved
     */
    public RDBManager(DataStore dataStore, String rdbFilePath) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null");
        }
        if (rdbFilePath == null || rdbFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("RDB file path cannot be null or empty");
        }
        
        this.dataStore = dataStore;
        this.rdbFilePath = rdbFilePath;
    }
    
    /**
     * Creates an RDBManager with directory and filename.
     * 
     * @param dataStore The data store to save
     * @param directory The directory where the RDB file will be saved
     * @param filename The RDB filename (e.g., "dump.rdb")
     */
    public RDBManager(DataStore dataStore, String directory, String filename) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null");
        }
        if (directory == null || directory.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory cannot be null or empty");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        this.dataStore = dataStore;
        this.rdbFilePath = Paths.get(directory, filename).toString();
    }
    
    /**
     * Saves the entire database to the RDB file.
     * 
     * Process:
     * 1. Get all keys from the data store
     * 2. Create the RDB file (or overwrite if exists)
     * 3. Write header
     * 4. Write SELECTDB for database 0
     * 5. Write all key-value pairs (currently only STRING types)
     * 6. Write EOF marker
     * 7. Write checksum
     * 
     * @throws IOException If an I/O error occurs
     * @throws RDBException If an RDB-specific error occurs
     */
    public void save() throws IOException, RDBException {
        // Ensure directory exists
        Path filePath = Paths.get(rdbFilePath);
        Path directory = filePath.getParent();
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        // Get all keys
        Set<String> keys = dataStore.getAllKeys();
        
        // Open file and write RDB format
        try (FileOutputStream fileOut = new FileOutputStream(rdbFilePath);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut)) {
            
            RDBWriter writer = new RDBWriter(bufferedOut);
            
            // 1. Write header: "REDIS0009"
            writer.writeHeader();
            
            // 2. Write SELECTDB for database 0
            writer.writeSelectDB(RDBConstants.DEFAULT_DATABASE);
            
            // 3. Write all key-value pairs
            int savedCount = 0;
            int skippedCount = 0;
            
            for (String key : keys) {
                RedisValue value = dataStore.getValue(key);
                
                // Skip if value is null (expired or deleted)
                if (value == null) {
                    skippedCount++;
                    continue;
                }
                
                // Skip if expired
                if (value.isExpired()) {
                    skippedCount++;
                    continue;
                }
                
                // Currently only support STRING type
                DataType type = dataStore.getType(key);
                if (type != DataType.STRING) {
                    skippedCount++;
                    continue; // Skip non-string types for now
                }
                
                try {
                    writer.writeKeyValuePair(key, value);
                    savedCount++;
                } catch (UnsupportedOperationException e) {
                    // Skip unsupported types
                    skippedCount++;
                }
            }
            
            // 4. Write EOF marker
            writer.writeEOF();
            
            // 5. Write checksum (zeros for now)
            writer.writeChecksum();
            
            // Flush to disk
            writer.flush();
            
            System.out.println("RDB save completed: " + savedCount + " keys saved, " + skippedCount + " keys skipped");
        }
    }
    
    /**
     * Loads an RDB file into the data store.
     * 
     * Process:
     * 1. Check if file exists
     * 2. Open file and read header
     * 3. Read opcodes and process them:
     *    - SELECTDB: Switch database (currently only DB 0 supported)
     *    - EXPIRETIME/EXPIRETIMEMS: Read expiry timestamp
     *    - VALUE_TYPE: Read key-value pair
     *    - EOF: End of file
     * 4. Read checksum
     * 5. Load all key-value pairs into DataStore
     * 
     * @throws IOException If an I/O error occurs
     * @throws RDBException If the RDB file format is invalid
     */
    public void load() throws IOException, RDBException {
        // Check if file exists
        Path filePath = Paths.get(rdbFilePath);
        if (!Files.exists(filePath)) {
            throw new RDBException("RDB file not found: " + rdbFilePath);
        }
        
        int loadedCount = 0;
        int skippedCount = 0;
        
        // Open file and read RDB format
        try (FileInputStream fileIn = new FileInputStream(rdbFilePath);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
            
            RDBReader reader = new RDBReader(bufferedIn);
            
            // 1. Read and validate header
            String version = reader.readHeader();
            System.out.println("Loading RDB file version: " + version);
            
            // 2. Read opcodes until EOF
            boolean eofReached = false;
            
            while (!eofReached) {
                int opcode = reader.readOpcode();
                
                if (opcode == -1) {
                    throw new RDBException("Unexpected end of stream before EOF marker");
                }
                
                switch (opcode) {
                    case RDBConstants.EOF_OPCODE:
                        // End of RDB file
                        eofReached = true;
                        break;
                        
                    case RDBConstants.SELECTDB_OPCODE:
                        // Database selector
                        int dbNumber = reader.readSelectDB();
                        if (dbNumber != 0) {
                            throw new RDBException("Only database 0 is supported, got database " + dbNumber);
                        }
                        break;
                        
                    case RDBConstants.EXPIRETIMEMS_OPCODE:
                        // Expiry time in milliseconds (applies to next key-value)
                        reader.readExpiryTimeMs();
                        break;
                        
                    case RDBConstants.EXPIRETIME_OPCODE:
                        // Expiry time in seconds (applies to next key-value)
                        reader.readExpiryTimeSec();
                        break;
                        
                    case RDBConstants.RESIZEDB_OPCODE:
                        // Resize DB hint - read two length-encoded values and ignore
                        RDBLengthEncoding.decodeLength(bufferedIn); // hash table size
                        RDBLengthEncoding.decodeLength(bufferedIn); // expire hash table size
                        break;
                        
                    case RDBConstants.AUX_OPCODE:
                        // Auxiliary field (metadata) - read and discard
                        reader.readAuxField();
                        break;
                        
                    default:
                        // Assume it's a value type opcode
                        if (isValueType(opcode)) {
                            try {
                                RDBReader.RDBKeyValue keyValue = reader.readKeyValue(opcode);
                                
                                // Check if expired
                                if (keyValue.hasExpiry() && keyValue.getExpiryTime() < System.currentTimeMillis()) {
                                    skippedCount++;
                                    continue; // Skip expired keys
                                }
                                
                                // Load into DataStore
                                loadKeyValue(keyValue);
                                loadedCount++;
                                
                            } catch (UnsupportedOperationException e) {
                                // Skip unsupported types
                                System.out.println("Skipping unsupported type: " + e.getMessage());
                                skippedCount++;
                            }
                        } else {
                            throw new RDBException("Unknown opcode: 0x" + Integer.toHexString(opcode));
                        }
                }
            }
            
            // 3. Read checksum
            reader.readChecksum();
            
            System.out.println("RDB load completed: " + loadedCount + " keys loaded, " + skippedCount + " keys skipped");
        }
    }
    
    /**
     * Checks if an opcode represents a value type.
     * 
     * @param opcode The opcode to check
     * @return true if it's a value type, false otherwise
     */
    private boolean isValueType(int opcode) {
        return opcode == RDBConstants.TYPE_STRING ||
               opcode == RDBConstants.TYPE_LIST ||
               opcode == RDBConstants.TYPE_SET ||
               opcode == RDBConstants.TYPE_ZSET ||
               opcode == RDBConstants.TYPE_HASH ||
               opcode == RDBConstants.TYPE_ZIPMAP ||
               opcode == RDBConstants.TYPE_ZIPLIST ||
               opcode == RDBConstants.TYPE_INTSET ||
               opcode == RDBConstants.TYPE_ZSET_ZIPLIST ||
               opcode == RDBConstants.TYPE_HASH_ZIPLIST ||
               opcode == RDBConstants.TYPE_LIST_QUICKLIST ||
               opcode == RDBConstants.TYPE_STREAM;
    }
    
    /**
     * Loads a key-value pair into the DataStore.
     * 
     * @param keyValue The key-value pair to load
     */
    private void loadKeyValue(RDBReader.RDBKeyValue keyValue) {
        switch (keyValue.getType()) {
            case STRING:
                String stringValue = (String) keyValue.getValue();
                StringValue redisValue;
                
                if (keyValue.hasExpiry()) {
                    redisValue = new StringValue(stringValue, keyValue.getExpiryTime());
                } else {
                    redisValue = new StringValue(stringValue);
                }
                
                dataStore.setValue(keyValue.getKey(), redisValue);
                break;
                
            case LIST:
            case STREAM:
                throw new UnsupportedOperationException("Type " + keyValue.getType() + " not yet supported");
                
            default:
                throw new UnsupportedOperationException("Unknown type: " + keyValue.getType());
        }
    }
    
    /**
     * Checks if the RDB file exists.
     * 
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists() {
        return Files.exists(Paths.get(rdbFilePath));
    }
    
    /**
     * Gets the full path of the RDB file.
     * 
     * @return The RDB file path
     */
    public String getFilePath() {
        return rdbFilePath;
    }

    /**
     * Finds all keys in the RDB file that match the given glob pattern.
     * Similar to Redis KEYS command, but reads from RDB file instead of memory.
     * 
     * Glob patterns:
     * - * matches any sequence of characters
     * - ? matches exactly one character
     * - [abc] matches one character from the set
     * - [a-z] matches one character from the range
     * - \x escapes special characters
     * 
     * Examples:
     * - "*" matches all keys
     * - "user:*" matches all keys starting with "user:"
     * - "user:??" matches "user:id", "user:10", etc.
     * - "user:[123]" matches "user:1", "user:2", "user:3"
     * 
     * @param pattern The glob pattern to match against
     * @return List of matching keys (empty list if no matches or file doesn't exist)
     */
    public List<String> findKeysInRDBFile(String pattern) {
        List<String> matchingKeys = new ArrayList<>();
        
        // Check if file exists
        if (!fileExists()) {
            System.err.println("RDB file not found: " + rdbFilePath);
            return matchingKeys;
        }
        
        try (FileInputStream fileIn = new FileInputStream(rdbFilePath);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
            
            RDBReader reader = new RDBReader(bufferedIn);
            
            // Read and validate header
            reader.readHeader();
            
            // Read opcodes until EOF
            boolean eofReached = false;
            
            while (!eofReached) {
                int opcode = reader.readOpcode();
                
                if (opcode == -1) {
                    break; // Unexpected end, but return what we found
                }
                
                switch (opcode) {
                    case RDBConstants.EOF_OPCODE:
                        eofReached = true;
                        break;
                        
                    case RDBConstants.SELECTDB_OPCODE:
                        reader.readSelectDB();
                        break;
                        
                    case RDBConstants.EXPIRETIMEMS_OPCODE:
                        reader.readExpiryTimeMs();
                        break;
                        
                    case RDBConstants.EXPIRETIME_OPCODE:
                        reader.readExpiryTimeSec();
                        break;
                        
                    case RDBConstants.RESIZEDB_OPCODE:
                        RDBLengthEncoding.decodeLength(bufferedIn);
                        RDBLengthEncoding.decodeLength(bufferedIn);
                        break;
                        
                    case RDBConstants.AUX_OPCODE:
                        // Auxiliary field (metadata) - read and discard
                        reader.readAuxField();
                        break;
                        
                    default:
                        if (isValueType(opcode)) {
                            try {
                                RDBReader.RDBKeyValue keyValue = reader.readKeyValue(opcode);
                                
                                // Check if key matches pattern
                                if (matchesGlobPattern(keyValue.getKey(), pattern)) {
                                    // Skip expired keys
                                    if (!keyValue.hasExpiry() || keyValue.getExpiryTime() >= System.currentTimeMillis()) {
                                        matchingKeys.add(keyValue.getKey());
                                    }
                                }
                                
                            } catch (Exception e) {
                                // Skip keys we can't read
                                System.err.println("Error reading key: " + e.getMessage());
                            }
                        }
                }
            }
            
        } catch (IOException | RDBException e) {
            System.err.println("Error reading RDB file: " + e.getMessage());
        }
        
        return matchingKeys;
    }
    
    /**
     * Matches a string against a Redis-style glob pattern.
     * 
     * @param text The text to match
     * @param pattern The glob pattern
     * @return true if the text matches the pattern
     */
    private boolean matchesGlobPattern(String text, String pattern) {
        // Convert glob pattern to regex
        String regex = globToRegex(pattern);
        return text.matches(regex);
    }
    
    /**
     * Converts a Redis glob pattern to a Java regex.
     * 
     * @param glob The glob pattern
     * @return The equivalent regex pattern
     */
    private String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        boolean inEscape = false;
        boolean inCharClass = false;
        
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            
            if (inEscape) {
                // Escape the character for regex
                regex.append(Pattern.quote(String.valueOf(c)));
                inEscape = false;
            } else if (c == '\\') {
                inEscape = true;
            } else if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append(".");
            } else if (c == '[') {
                regex.append('[');
                inCharClass = true;
            } else if (c == ']' && inCharClass) {
                regex.append(']');
                inCharClass = false;
            } else if (inCharClass) {
                // Inside character class, keep as-is
                regex.append(c);
            } else {
                // Regular character - escape regex special chars
                if ("(){}+|^$.".indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
            }
        }
        
        regex.append("$");
        return regex.toString();
    }
}
