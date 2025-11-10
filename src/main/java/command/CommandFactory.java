package command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.handlers.connection.*;
import command.handlers.geospatial.GEOADDCommand;
import command.handlers.list.*;
import command.handlers.pubsub.*;
import command.handlers.replication.*;
import command.handlers.sortedset.*;
import command.handlers.stream.*;
import command.handlers.string.*;
import command.handlers.transaction.*;
import replication.ReplicationManager;
import storage.DataStore;
import replication.sync.WaitRequestManager;

public class CommandFactory {
    private final DataStore dataStore;
    private final Map<String, CommandStrategy> commandMapCache = new ConcurrentHashMap<>();
    private final ReplicationManager replicationManager;
    private final WaitRequestManager WaitRequestManager;

    public CommandFactory(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
        this.WaitRequestManager = new WaitRequestManager(replicationManager);
    }

    public CommandStrategy getCommandStrategy(String commandName) {
        return commandMapCache.computeIfAbsent(commandName.toUpperCase(), this::createCommandInstance);
    }

    private CommandStrategy createCommandInstance(String commandName) {
        return switch (commandName.toUpperCase()) {
            case "PING" -> new PingHandler();
            case "ECHO" -> new EchoHandler();
            case "SET" -> new SetHandler(dataStore, replicationManager);
            case "GET" -> new GetHandler(dataStore);
            case "CONFIG" -> new CONFIGHandler();
            case "RPUSH" -> new RPUSHHandler(dataStore, replicationManager);
            case "LRANGE" -> new LRANGEHandler(dataStore);
            case "LPUSH" -> new LPUSHHandler(dataStore, replicationManager);
            case "LLEN" -> new LLENHandler(dataStore);
            case "LPOP" -> new LPOPHandler(dataStore, replicationManager);
            case "BLPOP" -> new BLPOPHandler(dataStore, replicationManager);
            case "TYPE" -> new TYPEHandler(dataStore);
            case "XADD" -> new XADDHandler(dataStore, replicationManager);
            case "XRANGE" -> new XRANGEHandler(dataStore);
            case "XREAD" -> new XREADHandler(dataStore);
            case "INCR" -> new INCRHandler(dataStore, replicationManager);
            case "MULTI" -> new MULTIHandler();
            case "EXEC" -> new EXECHandler();
            case "DISCARD" -> new DISCARDHandler();
            case "SAVE" -> new SAVEHandler(dataStore);
            case "INFO" -> new INFOHandler(replicationManager);
            case "LISTENING-PORT" -> new ListeningPortHandler(replicationManager);
            case "CAPA" -> new CapaHandler();
            case "PSYNC" -> new PSYNCHandler(replicationManager);
            case "GETACK" -> new GetAckHandler(replicationManager);
            case "ACK" -> new AckHandler(WaitRequestManager, replicationManager);
            case "WAIT" -> new WaitHandler(replicationManager, WaitRequestManager);
            case "KEYS" -> new KEYHandler(dataStore);
            case "SUBSCRIBE" -> new SubscribeHandler();
            case "PUBLISH" -> new PublishHandler();
            case "UNSUBSCRIBE" -> new UnsubscribeHandler();
            case "ZADD" -> new ZADDHandler(dataStore);
            case "ZRANK" -> new ZRANKHandler(dataStore);
            case "ZRANGE" -> new ZRANGEHandler(dataStore);
            case "ZCARD" -> new ZCARDHandler(dataStore);
            case "ZSCORE" -> new ZSCOREHandler(dataStore);
            case "ZREM" -> new ZREMHandler(dataStore);
            case "GEOADD" -> new GEOADDCommand(dataStore);
            default -> null;
        };
    }
}
