package command.ResponseWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ResponseWriter {
    private final BufferedWriter writer;
    private final OutputStream outputStream;
    
    public ResponseWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }
    
    // For text-based commands
    public void write(String response) throws IOException {
        writer.write(response);
    }

    public void flush() throws IOException {
        writer.flush();
    }
    
    // For binary commands like PSYNC
    public void writeBytes(byte[] data) throws IOException {
        writer.flush(); // Flush any pending text first
        outputStream.write(data);
        outputStream.flush();
    }
    
    public BufferedWriter getWriter() {
        return writer;
    }
    
    public OutputStream getOutputStream() {
        return outputStream;
    }
}