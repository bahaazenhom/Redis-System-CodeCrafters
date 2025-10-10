
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import command.CommandExecuter;
import server.ClientHandler;
import storage.impl.InMemoryDataStore;

public class Main {
    public static void main(String[] args) throws IOException {
        CommandExecuter commandExecuter = new CommandExecuter(new InMemoryDataStore());
        System.out.println("Logs from your program will appear here!");
        int port = 6379;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            // Continuously accept new clients
            while (true) {
                System.out.println("Waiting for clients to connect...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                new Thread(new ClientHandler(clientSocket, commandExecuter)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter clientOutput = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                
                ) {
            Map<String, String> dataStore = new HashMap<>();
            String content;
            while ((content = clientInput.readLine()) != null) {
                if(content.equalsIgnoreCase("ping")) {
                    clientOutput.write("+PONG\r\n");
                    clientOutput.flush();
                } else if(content.equalsIgnoreCase("echo")) {
                    String numBytes = clientInput.readLine();
                    clientOutput.write(numBytes + "\r\n" + clientInput.readLine() + "\r\n");
                    clientOutput.flush();
                }
                else if(content.equalsIgnoreCase("set")){
                    String key = clientInput.readLine();
                    String value = clientInput.readLine();
                    dataStore.put(key, value); 
                    clientOutput.write("+OK\r\n");
                    clientOutput.flush();

                }
                else if(content.equalsIgnoreCase("get")){
                    String key = clientInput.readLine();
                    String value = dataStore.get(key);
                    if(value != null)clientOutput.write(value.length()+"\r\n"+value+"\r\n");
                    else clientOutput.write("$-1\r\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}
