package command;

import protocol.RESPSerializer;
import storage.DataStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import command.transactions.TransactionCoordinator;
import command.transactions.TransactionManager;

/**
 * CommandExecuter is responsible for executing commands.
 * Transaction logic is delegated to TransactionCoordinator.
 */
public class CommandExecuter {
    private final Map<String, CommandStrategy> commandMap = new HashMap<>();
    private final CommandFactory commandFactory;
    private final TransactionCoordinator transactionCoordinator;

    public CommandExecuter(DataStore dataStore) {
        this.commandFactory = new CommandFactory(dataStore);
        TransactionManager transactionManager = new TransactionManager();
        this.transactionCoordinator = new TransactionCoordinator(transactionManager, commandFactory);
    }

    public void register(String commandName, CommandStrategy command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public void execute(String clientId, String commandName, List<String> arguments, BufferedWriter clientOutput) {
        CommandStrategy command = commandFactory.getCommandStrategy(commandName);

        if (command != null) {
            try {
                // Delegate transaction control commands to TransactionCoordinator
                if (transactionCoordinator.isTransactionControlCommand(commandName)) {
                    transactionCoordinator.handleTransactionControlCommand(clientId, commandName, arguments, command,
                            clientOutput);
                    return;
                }

                // If client is in MULTI mode, queue the command instead of executing
                if (transactionCoordinator.isInMultiMode(clientId)) {
                    transactionCoordinator.queueCommand(clientId, commandName, arguments, command, clientOutput);
                    return;
                }

                // Normal execution path: validate and execute
                command.validateArguments(arguments);
                command.execute(arguments, clientOutput);

            } catch (IllegalArgumentException e) {
                try {
                    clientOutput.write(RESPSerializer.error(e.getMessage()));
                    clientOutput.flush();
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Unknown command
            try {
                clientOutput.write(RESPSerializer.error("unknown command '" + commandName + "'"));
                clientOutput.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
