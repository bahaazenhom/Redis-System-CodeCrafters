package command;

import protocol.RESPSerializer;
import storage.DataStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import command.transactions.TransactionManager;

public class CommandExecuter {
    private final Map<String, CommandStrategy> commandMap = new HashMap<>();
    private final CommandFactory commandFactory;
    private final TransactionManager transactionManager = new TransactionManager();

    public CommandExecuter(DataStore dataStore) {
        this.commandFactory = new CommandFactory(dataStore, transactionManager);
    }

    public void register(String commandName, CommandStrategy command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public void execute(String clientId, String commandName, List<String> arguments, BufferedWriter clientOutput) {
        CommandStrategy command = commandFactory.getCommandStrategy(commandName);
        if (command != null) {
            try {
                if (transactionManager.isInMultiMode(clientId)) {
                    try {
                        command.validateArguments(arguments);
                    } catch (IllegalArgumentException e) {
                        try {
                            clientOutput.write(RESPSerializer
                                    .error("ERR EXECABORT Transaction discarded because of previous errors."));
                            clientOutput.flush();
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                        return;
                    }
                    transactionManager.enqueueCommand(clientId, new CommandRequest(commandName, arguments));
                    try{
                    clientOutput.write(RESPSerializer.bulkString("QUEUED"));
                    clientOutput.flush();
                    return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(commandName.equalsIgnoreCase("MULTI")) {
                    arguments.add(clientId);
                }
                command.validateArguments(arguments);
                command.execute(arguments, clientOutput);
            } catch (IllegalArgumentException e) {
                try {
                    clientOutput.write(RESPSerializer.error(e.getMessage()));
                    clientOutput.flush();
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            }
        } else {
            try {
                clientOutput.write(RESPSerializer.error("unknown command '" + commandName + "'"));
                clientOutput.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
