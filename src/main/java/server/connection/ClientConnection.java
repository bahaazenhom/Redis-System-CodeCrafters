package server.connection;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ClientConnection {

    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    
    // Flag to indicate PSYNC completed - ClientHandler should stop reading
    private volatile boolean handoverToSlaveAckHandler = false;

    public ClientConnection(OutputStream outputStream, InputStream inputStream) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    /* ========== WRITE ========== */

    public synchronized void write(String response) throws IOException {
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
       // writer.write(response);
    }

    public synchronized void flush() throws IOException {
        outputStream.flush();
    }

    public synchronized void writeBytes(byte[] data) throws IOException {
        outputStream.write(data);
    }

    public synchronized void flushBytes() throws IOException {
        outputStream.flush();
    }

    /* ========== READ (simple) ========== */

    // Read a single character (like InputStream.read())
    public int read() throws IOException {
        return inputStream.read();
    }

    // Read a single line terminated by '\n' (like BufferedReader.readLine())
    public String readLine() throws IOException {
        return reader.readLine();
    }

    // Read raw bytes into a buffer
    public int readBytes(byte[] buffer) throws IOException {
        return inputStream.read(buffer);
    }

    /* ========== CONNECTION MGMT ========== */

    public void close() {
        try { reader.close(); } catch (IOException ignored) {}
        try { inputStream.close(); } catch (IOException ignored) {}
        try { outputStream.close(); } catch (IOException ignored) {}
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public BufferedReader getBufferedReader() {
        return reader;
    }
    
    /* ========== PSYNC HANDOVER ========== */
    
    /**
     * Mark this connection as handed over to SlaveAckHandler after PSYNC.
     * ClientHandler should stop reading after this is set.
     */
    public void markHandoverToSlaveAckHandler() {
        this.handoverToSlaveAckHandler = true;
    }
    
    /**
     * Check if PSYNC completed and SlaveAckHandler took over.
     */
    public boolean isHandoverToSlaveAckHandler() {
        return handoverToSlaveAckHandler;
    }
}
