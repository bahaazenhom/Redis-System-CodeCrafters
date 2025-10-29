package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPParser;
import protocol.RESPSerializer;
import replication.ReplicationManager;

public class CommandPropagationHandler implements Runnable {

    private final ReplicationManager replicationManager = ReplicationManager.create();
    private final ClientConnection responseWriter;
    private final CommandExecuter commandExecuter;
    private final BufferedReader in;

    public CommandPropagationHandler(CommandExecuter commandExecuter, ClientConnection responseWriter, BufferedReader in) {
        this.commandExecuter = commandExecuter;
        this.responseWriter = responseWriter;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            processCommands(in, responseWriter);
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void processCommands(BufferedReader in, ClientConnection responseWriter)
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("--------------------------------ReplicaHandler received line: " + line);
            if (line.isEmpty() || !line.startsWith("*"))
                continue;

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);

            String commandName = commands.get(0);
            
            int startIndexSublist = 1;
            if (commandName.equalsIgnoreCase("REPLCONF")) {
                commandName += " " + commands.get(1);
                startIndexSublist = 2;
            }
            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            System.out.println("Received command: " + commandName + " with arguments: " + arguments);
            commandExecuter.execute("clientId", commandName, arguments, responseWriter);

            // Update replication offset
            String RESPCommand = RESPSerializer.array(commands);
            replicationManager.updateSlaveOffset(RESPCommand.getBytes().length);
        }
    }

}
