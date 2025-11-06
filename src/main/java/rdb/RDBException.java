package rdb;

/**
 * Exception thrown when an error occurs during RDB file operations.
 */
public class RDBException extends Exception {
    
    /**
     * Constructs a new RDBException with the specified detail message.
     * 
     * @param message The detail message
     */
    public RDBException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new RDBException with the specified detail message and cause.
     * 
     * @param message The detail message
     * @param cause The cause
     */
    public RDBException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new RDBException with the specified cause.
     * 
     * @param cause The cause
     */
    public RDBException(Throwable cause) {
        super(cause);
    }
}
