package command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import command.impl.acks.AckCommand;
import command.impl.acks.GetAckCommand;
import command.impl.connection.*;
import command.impl.handshake.CapaCommand;
import command.impl.handshake.ListeningPortCommand;
import command.impl.handshake.PSYNCCommand;
import command.impl.query.*;
import command.impl.replication.WaitCommand;
import command.impl.sub.pub.PublishCommand;
import command.impl.sub.pub.SubscribeCommand;
import command.impl.transaction.*;
import command.impl.writecommands.*;
import replication.ReplicationManager;
import storage.DataStore;
import storage.concurrency.waitcommandmanagement.AcksWaitManager;

public class CommandFactory {
    private final DataStore dataStore;
    private final Map<String, CommandStrategy> commandMapCache = new ConcurrentHashMap<>();
    private final ReplicationManager replicationManager;
    private final AcksWaitManager acksWaitManager;

    public CommandFactory(DataStore dataStore, ReplicationManager replicationManager) {
        this.dataStore = dataStore;
        this.replicationManager = replicationManager;
        this.acksWaitManager = new AcksWaitManager(replicationManager);
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
            case "CONFIG" -> new CONFIGCommand();
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
            case "SAVE" -> new SAVECommand(dataStore);
            case "INFO" -> new INFOCommand(replicationManager);
            case "LISTENING-PORT" -> new ListeningPortCommand(replicationManager);
            case "CAPA" -> new CapaCommand();
            case "PSYNC" -> new PSYNCCommand(replicationManager);
            case "GETACK" -> new GetAckCommand(replicationManager);
            case "ACK" -> new AckCommand(acksWaitManager, replicationManager);
            case "WAIT" -> new WaitCommand(replicationManager, acksWaitManager);
            case "KEYS" -> new KEYCommand(dataStore);
            case "SUBSCRIBE" -> new SubscribeCommand(dataStore);
            case "PUBLISH" -> new PublishCommand(dataStore);
            default -> null;
        };
    }
}
