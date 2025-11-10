package command.handlers.replication;

import java.util.List;

import command.CommandStrategy;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import replication.sync.WaitRequestManager;

public class AckHandler implements CommandStrategy {
    private final WaitRequestManager WaitRequestManager;
    private final ReplicationManager replicationManager;

    public AckHandler(WaitRequestManager WaitRequestManager, ReplicationManager replicationManager) {
        this.WaitRequestManager = WaitRequestManager;
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String ackOffsetValue = arguments.get(0);
            long ackOffset = Long.parseLong(ackOffsetValue);

            Integer slavePort = replicationManager.getSlaveIdForConnection(clientOutput);
            System.out.println("[AckCommand] Received ACK " + ackOffset + " from connection " + clientOutput + " mapped to slave " + slavePort);
            if (slavePort == null) {
                System.out.println("Received ACK from an unregistered replica connection, ignoring.");
                return;
            }

            WaitRequestManager.signalWaiters(slavePort, ackOffset);
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
