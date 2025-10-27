package command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.impl.*;
import replication.ReplicationManager;
import storage.DataStore;

public class CommandFactory {
    private final DataStore dataStore;
    private final Map<String, CommandStrategy> commandMapCache = new ConcurrentHashMap<>();
    private final ReplicationManager replicationManager;

    public CommandFactory(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
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
            case "INCR" -> new INCRCommand(dataStore);
            case "MULTI" -> new MULTICommand();
            case "EXEC" -> new EXECCommand();
            case "DISCARD" -> new DISCARDCommand();
            case "INFO" -> new INFOCommand(replicationManager);
            default -> null;
        };
    }
}
