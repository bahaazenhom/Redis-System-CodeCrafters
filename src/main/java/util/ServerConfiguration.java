package util;

/**
 * ServerConfiguration parses and holds Redis server startup configuration
 * from command-line arguments.
 * 
 * Supported arguments:
 * - --port <port>: Server port (default: 6379)
 * - --replicaof <host> <port>: Configure as replica of specified master
 * - --dir <path>: Directory for RDB file
 * - --dbfilename <name>: RDB file name
 */
public class ServerConfiguration {
    // Constants
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOST = "localhost";
    private static final String ROLE_MASTER = "master";
    private static final String ROLE_SLAVE = "slave";
    
    // Configuration fields
    private final int port;
    private final String serverRole;
    private final String masterHost;
    private final int masterPort;
    private final String rdbFileDir;
    private final String rdbFileName;

    public ServerConfiguration(String[] args) {
        ConfigBuilder builder = new ConfigBuilder();
        parseArgs(args, builder);
        
        this.port = builder.port;
        this.serverRole = builder.serverRole;
        this.masterHost = builder.masterHost;
        this.masterPort = builder.masterPort;
        this.rdbFileDir = builder.rdbFileDir;
        this.rdbFileName = builder.rdbFileName;
    }

    private void parseArgs(String[] args, ConfigBuilder builder) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--port":
                    if (i + 1 < args.length) {
                        builder.port = parsePort(args[++i]);
                    }
                    break;
                    
                case "--replicaof":
                    // Format: --replicaof <master_host> <master_port>
                    if (i + 2 < args.length) {
                        builder.serverRole = ROLE_SLAVE;
                        builder.masterHost = args[++i];
                        builder.masterPort = parsePort(args[++i]);
                    }
                    break;
                    
                case "--dir":
                    if (i + 1 < args.length) {
                        builder.rdbFileDir = args[++i];
                    }
                    break;
                    
                case "--dbfilename":
                    if (i + 1 < args.length) {
                        builder.rdbFileName = args[++i];
                    }
                    break;
            }
        }
    }
    
    private int parsePort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + portStr);
        }
    }

    // Getters
    public int getPort() {
        return port;
    }

    public String getServerRole() {
        return serverRole;
    }
    
    public boolean isMaster() {
        return ROLE_MASTER.equals(serverRole);
    }
    
    public boolean isSlave() {
        return ROLE_SLAVE.equals(serverRole);
    }

    public String getMasterHost() {
        return masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public String getRdbFileDir() {
        return rdbFileDir;
    }

    public String getRdbFileName() {
        return rdbFileName;
    }
    
    // Helper builder class
    private static class ConfigBuilder {
        int port = DEFAULT_PORT;
        String serverRole = ROLE_MASTER;
        String masterHost = DEFAULT_HOST;
        int masterPort = 0;
        String rdbFileDir = null;
        String rdbFileName = null;
    }
}