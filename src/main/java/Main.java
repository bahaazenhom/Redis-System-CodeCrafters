import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
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
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter clientOutput = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));) {
            Map<String, String> dataStore = new HashMap<>();
            while (clientInput.readLine() != null) {
                    clientInput.readLine();
                    String current = clientInput.readLine();
                    if (current.equalsIgnoreCase("ping")) {
                        clientOutput.write("+PONG\r\n");
                        clientOutput.flush();
                    } else if (current.equalsIgnoreCase("echo")) {
                        String numBytes = clientInput.readLine();
                        clientOutput.write(numBytes + "\r\n" + clientInput.readLine() + "\r\n");
                        clientOutput.flush();
                    } else if (current.equalsIgnoreCase("set")) {
                        clientInput.readLine();
                        String key = clientInput.readLine();
                        clientInput.readLine();
                        String value = clientInput.readLine();
                        dataStore.put(key, value);
                        clientOutput.write("+OK\r\n");
                        clientOutput.flush();

                    } else if (current.equalsIgnoreCase("get")) {
                        clientInput.readLine();
                        String key = clientInput.readLine();
                        String value = dataStore.get(key);
                        if (value != null) {
                            clientOutput.write("$" + value.length() + "\r\n" + value + "\r\n");
                            clientOutput.flush();
                        } else {
                            clientOutput.write("$-1\r\n");
                            clientOutput.flush();
                        }
                    }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
