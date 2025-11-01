package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPParser;
import util.AppLogger;

public class SlaveAckHandler{

    private static final Logger log = AppLogger.getLogger(SlaveAckHandler.class);
    
    private final ClientConnection clientConnection;
    private final CommandExecuter commandExecuter;
    private final String clientId;

    public SlaveAckHandler(ClientConnection clientConnection, CommandExecuter commandExecuter) {
        this.clientConnection = clientConnection;
        this.commandExecuter = commandExecuter;
        this.clientId = UUID.randomUUID().toString();
        log.info("Constructor called for clientId: " + clientId + ", connection: " + clientConnection);
    }

    public void run() {
        try {
            log.info("run() method STARTED for clientId: " + clientId);
            processCommands(clientConnection);
            log.info("run() method COMPLETED for clientId: " + clientId);
        } catch (IOException e) {
            log.severe("Client connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processCommands(ClientConnection clientConnection)
            throws IOException {
        log.info("Starting to process commands for clientId: " + clientId);
        BufferedReader in = clientConnection.getBufferedReader();
        log.info("Got BufferedReader: " + in + ", about to enter readLine loop");
        String line;
        while ((line = in.readLine()) != null) {
            log.info("*** READ LINE: \"" + line + "\"");
            
            if (line.isEmpty() || !line.startsWith("*")) {
                log.fine("Skipping line (empty or not array)");
                continue;
            }
            
            log.fine("Processing RESP array");
            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);
            log.info("Parsed commands: " + commands);
            
            String commandName = commands.get(0);

            int startIndexSublist = 1;
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            log.info("Executing command: " + commandName + " with args: " + arguments);
            commandExecuter.execute(clientId, commandName, arguments, clientConnection);
        }
        log.info("Exited readLine loop - stream closed");
    }

}
