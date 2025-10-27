package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import command.CommandExecuter;
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
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()))) {

            System.out.println("server port is: " + socket.getLocalPort());
            processCommands(in, out);

        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void processCommands(BufferedReader in, BufferedWriter out)
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() || !line.startsWith("*"))
                continue;

            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);

            String commandName = commands.get(0);

            // if (commandName.equals("-p")) {
            //     List<String> arguments2 = new ArrayList<>();
            //     commandName = commands.get(2);// second arg is actual command
            //     arguments2.add(commandName);
            //     arguments2.add(commands.get(1));// first arg is port
            //     arguments2.addAll(commands.subList(3, commands.size()));// rest are args
            //     commands = arguments2; // update arguments
            // }

            List<String> arguments = commands.subList(1, commands.size());
            System.out.println("Received command: " + commandName + " with arguments: " + arguments);
            commandExecuter.execute(clientId, commandName, arguments, out);
        }
    }

}
