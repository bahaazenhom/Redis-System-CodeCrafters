package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;

/**
 * MULTI command implementation.
 * Note: The actual transaction logic is handled in CommandExecuter.
 * This class only provides validation - execution is intercepted by CommandExecuter.
 */
public class MULTICommand implements CommandStrategy {

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 0) {
            throw new IllegalArgumentException("ERR wrong number of arguments for 'multi' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        // This method should never be called because MULTI is intercepted
        // in CommandExecuter.handleTransactionControlCommand()
        throw new UnsupportedOperationException("MULTI command should be handled by CommandExecuter");
    }
}