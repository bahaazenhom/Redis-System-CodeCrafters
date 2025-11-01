package command;

import java.util.List;

import server.connection.ClientConnection;

public interface CommandStrategy {
    void execute(List<String> arguments, ClientConnection clientOutput);
    void validateArguments(List<String> arguments) throws IllegalArgumentException;
}
