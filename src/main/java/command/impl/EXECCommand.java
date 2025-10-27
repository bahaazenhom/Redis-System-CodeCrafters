package command.impl;

import java.io.BufferedWriter;
import java.util.List;

import command.CommandStrategy;

/**
 * EXEC command implementation.
 * Note: The actual transaction execution logic is handled in CommandExecuter.
 * This class only provides validation - execution is intercepted by CommandExecuter.
 */
public class EXECCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        // This method should never be called because EXEC is intercepted
        // in CommandExecuter.handleTransactionControlCommand()
        throw new UnsupportedOperationException("EXEC command should be handled by CommandExecuter");
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 0) {
            throw new IllegalArgumentException("ERR wrong number of arguments for 'exec' command");
        }
    }
}
