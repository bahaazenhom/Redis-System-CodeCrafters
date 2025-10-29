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

            ClientConnection responseWriter = new ClientConnection(outputStream);

            processCommands(in, responseWriter);
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void processCommands(BufferedReader in, ClientConnection responseWriter)
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() || !line.startsWith("*"))
                continue;

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);

            int startIndexSublist = 1;
            String commandName = commands.get(0);
            System.out.println(commandName.equalsIgnoreCase("REPLCONF"));
            if(commandName.equalsIgnoreCase("REPLCONF")){
                commandName += " " + commands.get(1);
                startIndexSublist = 2;
            }

            List<String> arguments = commands.subList(startIndexSublist, commands.size());
            System.out.println("Received command: " + commandName + " with arguments: " + arguments);
            commandExecuter.execute(clientId, commandName, arguments, responseWriter);
        }
    }

}
