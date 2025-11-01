package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import storage.concurrency.waitcommandmanagement.*;

public class WaitCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;
    private final AcksWaitManager acksWaitManager;

    public WaitCommand(ReplicationManager replicationManager, AcksWaitManager acksWaitManager) {
        this.replicationManager = replicationManager;
        this.acksWaitManager = acksWaitManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            int numAcksRequired = Integer.parseInt(arguments.get(0));
            long timeoutMillis = Long.parseLong(arguments.get(1));
            if(replicationManager.getMasterNode().getOffset() == 0){
                clientOutput.write(RESPSerializer.integer(replicationManager.getSlaveNodesSockets().size()));
                clientOutput.flush();
                return;
            }
            long targetOffset = replicationManager.getMasterNode().getOffset();
            System.out.println("[WaitCommand] Waiting for " + numAcksRequired + " replicas at offset " + targetOffset
                    + " within " + timeoutMillis + " ms");
            WaitRequest req = new WaitRequest(targetOffset, numAcksRequired, timeoutMillis);
            int acknowledgedReplicas = acksWaitManager.awaitClientForAcks(req);
            clientOutput.write(RESPSerializer.integer(acknowledgedReplicas));
            clientOutput.flush();
        }
        catch (Exception e) {
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
