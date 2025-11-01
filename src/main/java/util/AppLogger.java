package util;

import java.util.logging.*;

/**
 * Centralized logging configuration for the Redis application.
 * Provides consistent logging across all components.
 */
public class AppLogger {
    
    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static boolean initialized = false;
    
    /**
     * Initialize the logging system with a custom formatter
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // Remove default handlers
        for (Handler handler : ROOT_LOGGER.getHandlers()) {
            ROOT_LOGGER.removeHandler(handler);
        }
        
        // Create console handler with custom formatter
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new RedisLogFormatter());
        
        ROOT_LOGGER.addHandler(consoleHandler);
        ROOT_LOGGER.setLevel(Level.INFO); // Change to Level.FINE or Level.FINEST for more detailed logs
        
        initialized = true;
    }
    
    /**
     * Get a logger for a specific class
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            initialize();
        }
        return Logger.getLogger(clazz.getName());
    }
    
    /**
     * Get a logger with a specific name
     */
    public static Logger getLogger(String name) {
        if (!initialized) {
            initialize();
        }
        return Logger.getLogger(name);
    }
    
    /**
     * Custom formatter for Redis logs
     */
    private static class RedisLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String level = record.getLevel().getName();
            String logger = record.getLoggerName();
            
            // Extract just the class name (not the full package)
            String[] parts = logger.split("\\.");
            String shortLogger = parts[parts.length - 1];
            
            String message = formatMessage(record);
            
            // Format: [LEVEL] [ClassName] message
            return String.format("[%s] [%s] %s%n", level, shortLogger, message);
        }
    }
    
    /**
     * Set global log level
     */
    public static void setLogLevel(Level level) {
        ROOT_LOGGER.setLevel(level);
        for (Handler handler : ROOT_LOGGER.getHandlers()) {
            handler.setLevel(level);
        }
    }
}
