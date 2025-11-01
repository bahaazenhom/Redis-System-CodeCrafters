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
            processCommands(clientConnection);
        } catch (IOException e) {
            // Connection closed or error
        }
    }

    private void processCommands(ClientConnection clientConnection)
            throws IOException {
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
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            commandExecuter.execute(clientId, commandName, arguments, clientConnection);
        }
    }

}
