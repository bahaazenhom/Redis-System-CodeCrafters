package command.ResponseWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ClientConnection {
    private final OutputStream outputStream;

    public ClientConnection(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    // For text-based commands (RESP messages)
    public void write(String response) throws IOException {
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    // For binary data (like RDB)
    public void writeBytes(byte[] data) throws IOException {
        outputStream.write(data);
    }

    public void flushBytes() throws IOException {
        outputStream.flush();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
