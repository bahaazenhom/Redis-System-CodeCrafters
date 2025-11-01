package server.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import command.CommandExecuter;
import protocol.RESPParser;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.connection.ClientConnection;

public class CommandPropagationHandler implements Runnable {

    private final ReplicationManager replicationManager = ReplicationManager.create();
    private final ClientConnection clientConnection;
    private final CommandExecuter commandExecuter;

    public CommandPropagationHandler(CommandExecuter commandExecuter, ClientConnection clientConnection) {
        this.commandExecuter = commandExecuter;
        this.clientConnection = clientConnection;
    }

    @Override
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
            if (line.isEmpty() || !line.startsWith("*"))
                continue;

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);

            String commandName = commands.get(0);
            
            int startIndexSublist = 1;
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName = commands.get(1);
                startIndexSublist = 2;
            }
            
            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            commandExecuter.execute("clientId", commandName, arguments, clientConnection);

            // Update replication offset
            String RESPCommand = RESPSerializer.array(commands);
            replicationManager.updateSlaveOffset(RESPCommand.getBytes().length);
        }
    }

}
