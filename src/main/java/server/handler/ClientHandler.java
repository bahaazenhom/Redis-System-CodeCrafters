package server.handler;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import command.CommandExecuter;
import protocol.RESPParser;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import util.AppLogger;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandExecuter commandExecuter;
    private final String clientId;
    private final ReplicationManager replicationManager = ReplicationManager.create();
    Logger logger = AppLogger.getLogger(ClientHandler.class);

    public ClientHandler(Socket socket, CommandExecuter commandExecuter) {
        this.socket = socket;
        this.commandExecuter = commandExecuter;
        this.clientId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        ClientConnection clientConnection = null;

        try {
            outputStream = socket.getOutputStream();
            clientConnection = new ClientConnection(clientId, outputStream, socket.getInputStream());

            processCommands(clientConnection);

        } catch (IOException e) {
            // Connection closed or error
        } finally {
            // Only close if this is NOT a replica connection that completed PSYNC
            // Replica connections are kept alive and handled by SlaveAckHandler
            try {
                if (outputStream != null && !isReplicaConnection(clientConnection)) {
                    outputStream.close();
                }
                if (!isReplicaConnection(clientConnection)) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    private boolean isReplicaConnection(ClientConnection clientConnection) {
        if (clientConnection == null) {
            return false;
        }
        // Check if this connection is registered as a replica
        return replicationManager.getSlaveIdForConnection(clientConnection) != null;
    }

    private void processCommands(ClientConnection clientConnection)
            throws IOException {
        logger.info("Processing commands for client: " + clientId);

        BufferedReader in = clientConnection.getBufferedReader();
        String line;

        while ((line = in.readLine()) != null) {

            if (line.isEmpty() || !line.startsWith("*")) {
                continue;
            }

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);

            String commandName = commands.get(0);
            int startIndexSublist = 1;

            // Handle REPLCONF subcommands
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());

            try {
                commandExecuter.execute(clientId, commandName, arguments, clientConnection);
            } catch (Exception e) {
                // Error executing command
            }

            // After PSYNC completes, ClientHandler should exit and let SlaveAckHandler take
            // over
            if (clientConnection.isHandoverToSlaveAckHandler()) {
                break;
            }
        }
    }

}
