package command.handlers.replication;

import java.util.List;
import java.util.logging.Logger;

import command.CommandStrategy;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import replication.sync.WaitRequestManager;
import replication.sync.WaitRequest;
import util.AppLogger;

public class WaitHandler implements CommandStrategy {
    private static final Logger log = AppLogger.getLogger(WaitHandler.class);
    private final ReplicationManager replicationManager;
    private final WaitRequestManager WaitRequestManager;

    public WaitHandler(ReplicationManager replicationManager, WaitRequestManager WaitRequestManager) {
        this.replicationManager = replicationManager;
        this.WaitRequestManager = WaitRequestManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            int numAcksRequired = Integer.parseInt(arguments.get(0));
            long timeoutMillis = Long.parseLong(arguments.get(1));
            
            long currentOffset = replicationManager.getMasterNode().getOffset();
            int replicaCount = replicationManager.getSlaveNodesSockets().size();
            
            log.info("WAIT command: need " + numAcksRequired + " ACKs, timeout=" + timeoutMillis 
                    + "ms, currentOffset=" + currentOffset + ", replicas=" + replicaCount);
            
            if(currentOffset == 0){
                log.info("Master offset is 0 - immediately returning replica count: " + replicaCount);
                clientOutput.write(RESPSerializer.integer(replicaCount));
                clientOutput.flush();
                return;
            }
            
            long targetOffset = currentOffset;
            WaitRequest req = new WaitRequest(targetOffset, numAcksRequired, timeoutMillis);
            int acknowledgedReplicas = WaitRequestManager.awaitClientForAcks(req);
            
            log.info("WAIT command completed: received " + acknowledgedReplicas + " ACKs");
            clientOutput.write(RESPSerializer.integer(acknowledgedReplicas));
            clientOutput.flush();
        }
        catch (Exception e) {
            log.severe("WAIT command failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if(arguments.size() != 2){
            throw new IllegalArgumentException("Wrong number of arguments for 'WAIT' command");
        }
        try {
            Integer.parseInt(arguments.get(0));
            Long.parseLong(arguments.get(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments for 'WAIT' command must be numbers");
        }
    }
    
}
