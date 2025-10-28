package command;

import java.util.List;

import command.ResponseWriter.ResponseWriter;

public interface CommandStrategy {
    void execute(List<String> arguments, ResponseWriter clientOutput);
    void validateArguments(List<String> arguments) throws IllegalArgumentException;
}
