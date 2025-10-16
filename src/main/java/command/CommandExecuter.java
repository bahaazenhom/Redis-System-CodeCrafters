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

    public CommandExecuter(DataStore dataStore) {
        register("PING", new PingCommand());
        register("ECHO", new EchoCommand());
        register("SET", new SetCommand(dataStore));
        register("GET", new GetCommand(dataStore));
        register("RPUSH", new RPUSHCommand(dataStore));
        register("LRANGE", new LRANGECommand(dataStore));
        register("LPUSH", new LPUSHCommand(dataStore));
        register("LLEN", new LLEN(dataStore));
        register("LPOP", new LPOP(dataStore));
    }

    public void register(String commandName, CommandStrategy command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public void execute(String commandName, List<String> arguments, BufferedWriter clientOutput) {
        CommandStrategy command = commandMap.get(commandName.toUpperCase());
        if (command != null) {
            command.execute(arguments, clientOutput);
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
