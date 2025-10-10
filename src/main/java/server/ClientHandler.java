package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import command.CommandExecuter;
import protocol.RESPParser;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandExecuter commandExecuter;

    public ClientHandler(Socket socket, CommandExecuter commandExecuter) {
        this.socket = socket;
        this.commandExecuter = commandExecuter;
    }

    @Override
    public void run() {
         try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()))) {
            
            processCommands(in, out);
            
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

        private void processCommands(BufferedReader in, BufferedWriter out) 
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() || !line.startsWith("*")) continue;
            
            int numElements = Integer.parseInt(line.substring(1));
            List<String> commands = RESPParser.parseRequest(numElements, in);
            
            System.out.println("Parsed commands: " + commands);
            
            String commandName = commands.get(0);
            List<String> arguments = commands.subList(1, commands.size());
            
            commandExecuter.execute(commandName, arguments, out);
        }
    }
    
}
