package protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RESPParser {

    public static List<String> parseRequest(int numElements, BufferedReader reader) throws IOException {
        List<String> result = new ArrayList<>();

        String line;
        
        for (int i = 0; i < numElements; i++) {
            line = reader.readLine();
            if (line == null || !line.startsWith("$")) {
                throw new IOException("Invalid RESP bulk string");
            }
            int strLength = Integer.parseInt(line.substring(1));
            char[] strChars = new char[strLength];
            int read = reader.read(strChars, 0, strLength);
            if (read != strLength) {
                throw new IOException("Could not read full bulk string");
            }
            reader.readLine(); // Read the trailing \r\n
            result.add(new String(strChars));
        }

        return result;
    }

}
