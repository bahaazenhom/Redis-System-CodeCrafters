package command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.impl.BLPOPCommand;
import command.impl.EchoCommand;
import command.impl.GetCommand;
import command.impl.LLENCommand;
import command.impl.LPOPCommand;
import command.impl.LPUSHCommand;
import command.impl.LRANGECommand;
import command.impl.PingCommand;
import command.impl.RPUSHCommand;
import command.impl.SetCommand;
import command.impl.TYPECommand;
import command.impl.XADDCommand;
import command.impl.XRANGECommand;
import command.impl.XREADCommand;
import storage.DataStore;

public class CommandFactory {
    private final DataStore dataStore;
    private final Map<String, CommandStrategy> commandMapCache = new ConcurrentHashMap<>();

    public CommandFactory(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public CommandStrategy getCommandStrategy(String commandName) {
        return commandMapCache.computeIfAbsent(commandName.toUpperCase(), this::createCommandInstance);
    }

    private CommandStrategy createCommandInstance(String commandName) {
        return switch (commandName.toUpperCase()) {
            case "PING" -> new PingCommand();
            case "ECHO" -> new EchoCommand();
            case "SET" -> new SetCommand(dataStore);
            case "GET" -> new GetCommand(dataStore);
            case "RPUSH" -> new RPUSHCommand(dataStore);
            case "LRANGE" -> new LRANGECommand(dataStore);
            case "LPUSH" -> new LPUSHCommand(dataStore);
            case "LLEN" -> new LLENCommand(dataStore);
            case "LPOP" -> new LPOPCommand(dataStore);
            case "BLPOP" -> new BLPOPCommand(dataStore);
            case "TYPE" -> new TYPECommand(dataStore);
            case "XADD" -> new XADDCommand(dataStore);
            case "XRANGE" -> new XRANGECommand(dataStore);
            case "XREAD" -> new XREADCommand(dataStore);
            default -> null;
        };
    }
}
