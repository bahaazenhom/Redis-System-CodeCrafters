package command;

import protocol.RESPSerializer;
import pub.sub.ChannelManager;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import storage.DataStore;

import java.io.IOException;
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
    private final ChannelManager channelManager;

    public CommandExecuter(DataStore dataStore) {
        this.commandFactory = new CommandFactory(dataStore, ReplicationManager.create());
        TransactionManager transactionManager = new TransactionManager();
        this.transactionCoordinator = new TransactionCoordinator(transactionManager, commandFactory);
        this.channelManager = ChannelManager.getInstance();
    }

    public void register(String commandName, CommandStrategy command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public void execute(String clientId, String commandName, List<String> arguments,
            ClientConnection clientConnection) {
        CommandStrategy command = commandFactory.getCommandStrategy(commandName);

        if (command != null) {
            try {

                // Check if the client is in subscribe mode
                if (channelManager.isInSubscribeMode(clientId) && !channelManager.isSubscribeModeCommand(commandName)){
                    clientConnection.write(RESPSerializer.error("ERR Can't execute '" + commandName.toLowerCase() + "': only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT / RESET are allowed in this context"));
                    clientConnection.flush();
                    return;
                }

                // Delegate transaction control commands to TransactionCoordinator
                if (transactionCoordinator.isTransactionControlCommand(commandName)) {
                    transactionCoordinator.handleTransactionControlCommand(clientId, commandName, arguments, command,
                            clientConnection);
                    return;
                }

                // If client is in MULTI mode, queue the command instead of executing
                if (transactionCoordinator.isInMultiMode(clientId)) {
                    transactionCoordinator.queueCommand(clientId, commandName, arguments, command, clientConnection);
                    return;
                }

                // Normal execution path: validate and execute
                command.validateArguments(arguments);
                command.execute(arguments, clientConnection);

            } catch (IllegalArgumentException e) {
                try {
                    clientConnection.write(RESPSerializer.error(e.getMessage()));
                    clientConnection.flush();
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Unknown command
            try {
                clientConnection.write(RESPSerializer.error("unknown command '" + commandName + "'"));
                clientConnection.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
