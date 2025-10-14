package protocol;

public class RESPSerializer {
    public static String simpleString(String message) {
        return "+" + message + "\r\n";
    }

    public static String error(String message) {
        return "-" + message + "\r\n";
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


}
