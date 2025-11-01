package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPParser;

public class SlaveAckHandler{

    private final ClientConnection clientConnection;
    private final CommandExecuter commandExecuter;
    private final String clientId;

    public SlaveAckHandler(ClientConnection clientConnection, CommandExecuter commandExecuter) {
        this.clientConnection = clientConnection;
        this.commandExecuter = commandExecuter;
        this.clientId = UUID.randomUUID().toString();
        System.out.println("[SlaveAckHandler] Constructor called for clientId: " + clientId + ", connection: " + clientConnection);
    }

    public void run() {
        try {
            System.out.println("[SlaveAckHandler] run() method STARTED for clientId: " + clientId);
            processCommands(clientConnection);
            System.out.println("[SlaveAckHandler] run() method COMPLETED for clientId: " + clientId);
        } catch (IOException e) {
            System.err.println("Client connection error from SlaveAckHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processCommands(ClientConnection clientConnection)
            throws IOException {
        System.out.println("[SlaveAckHandler] Starting to process commands for clientId: " + clientId);
        BufferedReader in = clientConnection.getBufferedReader();
        System.out.println("[SlaveAckHandler] Got BufferedReader, about to enter readLine loop");
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("[SlaveAckHandler] *** READ LINE: \"" + line + "\"");
            
            if (line.isEmpty() || !line.startsWith("*")) {
                System.out.println("[SlaveAckHandler] Skipping line (empty or not array)");
                continue;
            }
            
            System.out.println("[SlaveAckHandler] Processing RESP array");
            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);
            System.out.println("[SlaveAckHandler] Parsed commands: " + commands);
            
            String commandName = commands.get(0);

            int startIndexSublist = 1;
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            System.out.println("[SlaveAckHandler] Executing command: " + commandName + " with args: " + arguments);
            commandExecuter.execute(clientId, commandName, arguments, clientConnection);
        }
        System.out.println("[SlaveAckHandler] Exited readLine loop - stream closed");
    }

}
