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
    }

    public void run() {
        try {
            System.out.println("Starting SlaveAckHandler for clientId: " + clientId);
            processCommands(clientConnection);
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void processCommands(ClientConnection clientConnection)
            throws IOException {
        System.out.println("SlaveAckHandler processing commands for clientId: " + clientId);
        BufferedReader in = clientConnection.getBufferedReader();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() || !line.startsWith("*"))
                continue;

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);
            System.out.println("--------------------------------SlaveAckHandler received line: " + line);
            String commandName = commands.get(0);

            int startIndexSublist = 1;
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            System.out.println("Received command from ack handler--------: " + commandName + " with arguments: " + arguments);
            commandExecuter.execute(clientId, commandName, arguments, clientConnection);
        }
    }

}
