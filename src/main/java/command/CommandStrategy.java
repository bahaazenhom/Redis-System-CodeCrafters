package command;

import java.util.List;

import command.ResponseWriter.ClientConnection;

public interface CommandStrategy {
    void execute(List<String> arguments, ClientConnection clientOutput);
    void validateArguments(List<String> arguments) throws IllegalArgumentException;
}
