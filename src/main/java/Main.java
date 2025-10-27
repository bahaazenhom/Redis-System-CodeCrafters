
import java.io.IOException;

import command.CommandExecuter;
import server.ServerManager;
import storage.impl.InMemoryDataStore;

public class Main {
    private static final int DEFAULT_PORT = 6379;

    public static void main(String[] args) throws IOException {

        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        ServerManager serverManager = ServerManager.create(commandExecuter);
        int port = parsePort(args); // Default Redis port
        String serverRole= parseServerRole(args);
        serverManager.startServer(port, serverRole);
    }

    private static String parseServerRole(String[] args) {
        for (String arg : args) {
            if (arg.equals("--replicaof")) {
                return "replica";
            }
        }
        return "master"; // default state
    }

    private static int parsePort(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                if (args[0].equals("--port")) {
                    port = Integer.parseInt(args[1]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
            }
        }
        return port;
    }
}
