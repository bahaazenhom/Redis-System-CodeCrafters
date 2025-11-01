package command.impl.acks;

import java.util.List;

import command.CommandStrategy;
import replication.ReplicationManager;
import server.connection.ClientConnection;

public class GetAckCommand implements CommandStrategy {
    ReplicationManager replicationManager;

    public GetAckCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientConnection) {
        try {
            String ackValue = arguments.get(0);
            System.out.println("Received REPLCONF GETACK with value: " + ackValue);
            System.out.println("[GetAckCommand] Replica replying with offset "
                    + replicationManager.getSlaveNode().getReplicationOffset());

            replicationManager.responseToMasterWithAckOffset(clientConnection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'REPLCONF GETACK' command");
        }
    }

}
