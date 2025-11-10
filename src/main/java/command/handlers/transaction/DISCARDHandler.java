package command.handlers.transaction;


import java.util.List;

import command.CommandStrategy;
import server.connection.ClientConnection;

/**
 * DISCARD command implementation.
 * Note: The actual transaction discard logic is handled in CommandExecuter.
 * This class only provides validation - execution is intercepted by CommandExecuter.
 */
public class DISCARDHandler implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        // This method should never be called because DISCARD is intercepted
        // in CommandExecuter.handleTransactionControlCommand()
        throw new UnsupportedOperationException("DISCARD command should be handled by CommandExecuter");
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 0) {
            throw new IllegalArgumentException("ERR wrong number of arguments for 'discard' command");
        }
    }
}
