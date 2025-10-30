package server;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import command.CommandExecuter;
import command.ResponseWriter.ClientConnection;
import protocol.RESPParser;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandExecuter commandExecuter;
    private final String clientId;

    public ClientHandler(Socket socket, CommandExecuter commandExecuter) {
        this.socket = socket;
        this.commandExecuter = commandExecuter;
        this.clientId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
                OutputStream outputStream = socket.getOutputStream()) {

            ClientConnection clientConnection = new ClientConnection(outputStream, socket.getInputStream());

            processCommands(in, clientConnection);
        } catch (IOException e) {
            System.err.println("Client connection error from : " + e.getMessage());
        }
    }
private void processCommands(BufferedReader in, ClientConnection clientConnection)
        throws IOException {

    String line;
    while ((line = in.readLine()) != null) {

        System.out.println("-------------------------------------------------");
        System.out.println("[PROPAGATION] Raw line received: \"" + line + "\"");

        if (line.isEmpty() || !line.startsWith("*")) {
            System.out.println("[PROPAGATION] Skipping line (empty or not array)");
            continue;
        }

        int numElements = Integer.parseInt(line.substring(1));
        System.out.println("[PROPAGATION] RESP array count: " + numElements);

        List<String> commands = RESPParser.parseRequest(numElements, in);
        System.out.println("[PROPAGATION] Parsed RESP elements: " + commands);

        String commandName = commands.get(0);
        System.out.println("[PROPAGATION] Initial commandName: " + commandName);

        int startIndexSublist = 1;
        if (commandName.equalsIgnoreCase("REPLCONF")) {
            System.out.println("[PROPAGATION] REPLCONF detected");
            commandName = commands.get(1);
            startIndexSublist = 2;
            System.out.println("[PROPAGATION] REPLCONF subcommand: " + commandName);
        }

        List<String> arguments = commands.subList(startIndexSublist, commands.size());
        System.out.println("[PROPAGATION] Final commandName: " + commandName + ", args: " + arguments);

        System.out.println("[PROPAGATION] Executing: " + commandName + " " + arguments);

        try {
            commandExecuter.execute(clientId, commandName, arguments, clientConnection);
            System.out.println("[PROPAGATION] Command executed successfully");
        } catch (Exception e) {
            System.out.println("[PROPAGATION] ERROR executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    System.out.println("[PROPAGATION] Input stream closed, exiting processCommands");
}


}
