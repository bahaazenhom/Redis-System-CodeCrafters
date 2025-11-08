package command.transactions;

import command.CommandFactory;
import command.CommandRequest;
import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionCoordinator handles all transaction-related operations.
 * Responsibilities:
 * - Managing transaction state (MULTI/EXEC/DISCARD)
 * - Queueing commands during transactions
 * - Executing queued commands atomically
 * - Collecting and returning replies
 */
public class TransactionCoordinator {
    private final TransactionManager transactionManager;
    private final CommandFactory commandFactory;

    public TransactionCoordinator(TransactionManager transactionManager, CommandFactory commandFactory) {
        this.transactionManager = transactionManager;
        this.commandFactory = commandFactory;
    }

    /**
     * Check if a command is a transaction control command (MULTI/EXEC/DISCARD)
     */
    public boolean isTransactionControlCommand(String commandName) {
        String upperCommand = commandName.toUpperCase();
        return upperCommand.equals("MULTI") || upperCommand.equals("EXEC") || upperCommand.equals("DISCARD");
    }

    /**
     * Check if the client is currently in MULTI mode
     */
    public boolean isInMultiMode(String clientId) {
        return transactionManager.isInMultiMode(clientId);
    }

    /**
     * Handle transaction control commands (MULTI/EXEC/DISCARD)
     */
    public void handleTransactionControlCommand(String clientId, String commandName, List<String> arguments,
            CommandStrategy command, ClientConnection clientConnection) throws IOException {

        String upperCommand = commandName.toUpperCase();

        switch (upperCommand) {
            case "MULTI":
                handleMulti(clientId, arguments, command, clientConnection);
                break;

            case "EXEC":
                handleExec(clientId, clientConnection);
                break;

            case "DISCARD":
                handleDiscard(clientId, clientConnection);
                break;
        }
    }

    /**
     * Queue a command for execution during EXEC
     */
    public void queueCommand(String clientId, String commandName, List<String> arguments,
            CommandStrategy command, ClientConnection clientConnection) throws IOException {
        try {
            // Validate the command before queuing
            command.validateArguments(arguments);

            // Enqueue the command
            transactionManager.enqueueCommand(clientId, new CommandRequest(commandName, arguments));

            // Send QUEUED response
            clientConnection.write(RESPSerializer.simpleString("QUEUED"));
            clientConnection.flush();
        } catch (IllegalArgumentException e) {
            // Validation error during queuing - discard transaction
            transactionManager.discardTransaction(clientId);
            clientConnection.write(RESPSerializer.error(e.getMessage()));
            clientConnection.flush();
        }
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private void handleMulti(String clientId, List<String> arguments, CommandStrategy command,
            ClientConnection clientConnection) throws IOException {
        // Check if already in MULTI mode
        if (transactionManager.isInMultiMode(clientId)) {
            clientConnection.write(RESPSerializer.error("MULTI calls can not be nested"));
            clientConnection.flush();
            return;
        }

        // Validate arguments (should be 0 arguments for MULTI)
        command.validateArguments(arguments);

        // Begin transaction
        transactionManager.beginTransactionContext(clientId);
        clientConnection.write(RESPSerializer.simpleString("OK"));
        clientConnection.flush();
    }

    private void handleExec(String clientId, ClientConnection clientConnection) throws IOException {
        // Check if in MULTI mode
        if (!transactionManager.isInMultiMode(clientId)) {
            clientConnection.write(RESPSerializer.error("EXEC without MULTI"));
            clientConnection.flush();
            return;
        }

        // Execute all queued commands atomically
        executeTransaction(clientId, clientConnection);
    }

    private void handleDiscard(String clientId, ClientConnection clientConnection) throws IOException {
        // Check if in MULTI mode
        if (!transactionManager.isInMultiMode(clientId)) {
            clientConnection.write(RESPSerializer.error("DISCARD without MULTI"));
            clientConnection.flush();
            return;
        }

        // Discard the transaction
        transactionManager.discardTransaction(clientId);
        clientConnection.write(RESPSerializer.simpleString("OK"));
        clientConnection.flush();
    }

    private void executeTransaction(String clientId, ClientConnection clientConnection) throws IOException {
        // Get all queued commands and clear the transaction context
        TransactionContext context = transactionManager.getTransactionContextAndClear(clientId);
        List<CommandRequest> queuedCommands = context.drainCommands();

        // Collect replies from each command execution
        List<String> replies = new ArrayList<>();

        // Execute each command and collect its reply
        for (CommandRequest request : queuedCommands) {
            try {
                // Use a StringWriter to capture the command output
                StringWriter stringWriter = new StringWriter();
                ClientConnection tempWriter = new ClientConnection(clientId, new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                        stringWriter.write(b);
                    }
                }, clientConnection.getInputStream());

                CommandStrategy commandStrategy = commandFactory.getCommandStrategy(request.getCommandName());
                if (commandStrategy != null) {
                    commandStrategy.execute(request.getArguments(), tempWriter);
                    replies.add(stringWriter.toString());
                } else {
                    replies.add(RESPSerializer.error("unknown command '" + request.getCommandName() + "'"));
                }
            } catch (Exception e) {
                // If a command fails during execution, return the error for that command
                replies.add(RESPSerializer.error(e.getMessage()));
            }
        }

        // Send all replies as a RESP array
        clientConnection.write(RESPSerializer.execArray(replies));
        clientConnection.flush();
    }
}
