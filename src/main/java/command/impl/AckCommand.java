package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import replication.ReplicationManager;
import storage.concurrency.waitcommandmanagement.AcksWaitManager;

public class AckCommand implements CommandStrategy {
    private final AcksWaitManager acksWaitManager;
    private final ReplicationManager replicationManager;

    public AckCommand(AcksWaitManager acksWaitManager, ReplicationManager replicationManager) {
        this.acksWaitManager = acksWaitManager;
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String ackOffsetValue = arguments.get(0);
            long ackOffset = Long.parseLong(ackOffsetValue);

            Integer slavePort = replicationManager.getSlaveIdForConnection(clientOutput);
            if (slavePort == null) {
                System.out.println("Received ACK from an unregistered replica connection, ignoring.");
                return;
            }

            acksWaitManager.signalWaiters(slavePort, ackOffset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'ACK' command");
        }
    }

}
