package command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.impl.*;
import command.impl.writecommands.*;
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
            case "SET" -> new SetCommand(dataStore, replicationManager);
            case "GET" -> new GetCommand(dataStore);
            case "RPUSH" -> new RPUSHCommand(dataStore, replicationManager);
            case "LRANGE" -> new LRANGECommand(dataStore);
            case "LPUSH" -> new LPUSHCommand(dataStore, replicationManager);
            case "LLEN" -> new LLENCommand(dataStore);
            case "LPOP" -> new LPOPCommand(dataStore, replicationManager);
            case "BLPOP" -> new BLPOPCommand(dataStore, replicationManager);
            case "TYPE" -> new TYPECommand(dataStore);
            case "XADD" -> new XADDCommand(dataStore, replicationManager);
            case "XRANGE" -> new XRANGECommand(dataStore);
            case "XREAD" -> new XREADCommand(dataStore);
            case "INCR" -> new INCRCommand(dataStore, replicationManager);
            case "MULTI" -> new MULTICommand();
            case "EXEC" -> new EXECCommand();
            case "DISCARD" -> new DISCARDCommand();
            case "INFO" -> new INFOCommand(replicationManager);
            case "REPLCONF" -> new REPLCONFCommand();
            case "PSYNC" -> new PSYNCCommand(replicationManager);
            default -> null;
        };
    }
}
