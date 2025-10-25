package command;

import java.io.BufferedWriter;
import java.util.List;

public interface CommandStrategy {
    void execute(List<String> arguments, BufferedWriter clientOutput);
    void validateArguments(List<String> arguments) throws IllegalArgumentException;
}
