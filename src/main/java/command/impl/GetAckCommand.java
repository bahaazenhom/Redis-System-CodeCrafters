package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import replication.ReplicationManager;

public class GetAckCommand implements CommandStrategy {
    ReplicationManager replicationManager;

    public GetAckCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try{
            String ackValue = arguments.get(0);
            System.out.println("Received REPLCONF GETACK with value: " + ackValue);
            replicationManager.responseToMasterWithAckOffset(clientOutput);
        }
        catch (Exception e) {
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
