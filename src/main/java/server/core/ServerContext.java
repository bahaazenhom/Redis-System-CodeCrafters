package server.core;

import util.ServerConfiguration;

/**
 * ServerContext is a singleton that holds server-wide state and configuration.
 * This provides a centralized access point for server configuration that can be
 * accessed by commands and other components without tight coupling.
 */
public class ServerContext {
    private static ServerContext instance;
    private ServerConfiguration configuration;

    private ServerContext() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance of ServerContext.
     */
    public static ServerContext getInstance() {
        if (instance == null) {
            synchronized (ServerContext.class) {
                if (instance == null) {
                    instance = new ServerContext();
                }
            }
        }
        return instance;
    }

    /**
     * Set the server configuration. Should be called once during server initialization.
     * 
     * @param configuration The server configuration
     */
    public void setConfiguration(ServerConfiguration configuration) {
        if (this.configuration != null) {
            throw new IllegalStateException("Server configuration has already been set");
        }
        this.configuration = configuration;
    }

    /**
     * Get the server configuration.
     * 
     * @return ServerConfiguration or null if not yet initialized
     */
    public ServerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Check if configuration has been initialized.
     */
    public boolean isConfigured() {
        return configuration != null;
    }
}
