package protocol;

import java.util.List;

public class RESPSerializer {
    public static String simpleString(String message) {
        return "+" + message + "\r\n";
    }

    public static String error(String message) {
        return "-ERR " + message + "\r\n";
    }

    public static String integer(long number) {
        return ":" + number + "\r\n";
    }

    public static String bulkString(String message) {
        if (message == null) {
            return nullBulkString();
        }
        return "$" + message.length() + "\r\n" + message + "\r\n";
    }

    public static String nullBulkString() {
        return "$-1\r\n"; // Null bulk string
    }

    public static String array(List<String> values) {
        StringBuilder respBuilder = new StringBuilder();
        respBuilder.append("*" + values.size() + "\r\n");
        for (String value : values)
            respBuilder.append(bulkString(value));
        return respBuilder.toString();
    }

    public static String xRangeArray(List<List<Object>> values) {
        if (values == null || values.isEmpty()) {
            return emptyArray();
        }
        StringBuilder respBuilder = new StringBuilder();
        respBuilder.append("*").append(values.size()).append("\r\n");

        for (List<Object> entry : values) {
            // Each entry is [entryId, [[field1, value1], [field2, value2], ...]]
            respBuilder.append("*2\r\n"); // Always 2 elements: ID and fields

            // First element: entry ID (bulk string)
            String entryId = (String) entry.get(0);
            respBuilder.append(bulkString(entryId));

            // Second element: array of field-value pairs
            Object fieldsObj = entry.get(1);
            if (fieldsObj == null) {
                respBuilder.append(emptyArray());
                continue;
            }
            
            @SuppressWarnings("unchecked")
            List<List<String>> fields = (List<List<String>>) fieldsObj;
            respBuilder.append("*").append(fields.size() * 2).append("\r\n");

            for (List<String> fieldPair : fields) {
                respBuilder.append(bulkString(fieldPair.get(0))); // field name
                respBuilder.append(bulkString(fieldPair.get(1))); // field value
            }
        }

        return respBuilder.toString();
    }

    @SuppressWarnings("unchecked") // tells the compiler to ignore warnings about unchecked type casts
    public static String xReadArray(List<List<Object>> values) {
        if (values == null || values.isEmpty()) {
            return nullArray();
        }
        StringBuilder respBuilder = new StringBuilder();
        respBuilder.append("*").append(values.size()).append("\r\n");
        for (List<Object> entry : values) {
            // Each entry is [streamKey, [[entryId1, fields1], [entryId2, fields2]]]
            respBuilder.append("*2\r\n"); // Always 2 elements: key and entries

            // First element: stream key (bulk string)
            String streamKey = (String) entry.get(0);
            respBuilder.append(bulkString(streamKey));

            // Second element: array of stream entries (use xRangeArray to serialize it)
            Object entriesObj = entry.get(1);
            if (entriesObj == null || (entriesObj instanceof List && ((List<?>) entriesObj).isEmpty())) {
                respBuilder.append(emptyArray());
            } else {
                List<List<Object>> entriesList = (List<List<Object>>) entriesObj;
                respBuilder.append(xRangeArray(entriesList));
            }
        }

        return respBuilder.toString();
    }

    public static String nullArray() {
        return "*-1\r\n"; // Null array
    }

    public static String emptyArray() {
        return "*0\r\n"; // Empty array
    }

}
