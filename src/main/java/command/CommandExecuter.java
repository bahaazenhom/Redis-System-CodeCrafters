package command;

import command.impl.*;
import protocol.RESPSerializer;
import storage.DataStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandExecuter {
    private final Map<String, CommandStrategy> commandMap = new HashMap<>();
    private final CommandFactory commandFactory;

    public CommandExecuter(DataStore dataStore) {
        this.commandFactory = new CommandFactory(dataStore);
    }

    public void register(String commandName, CommandStrategy command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public void execute(String commandName, List<String> arguments, BufferedWriter clientOutput) {
        CommandStrategy command = commandFactory.getCommandStrategy(commandName);
        if (command != null) {
            try {
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
