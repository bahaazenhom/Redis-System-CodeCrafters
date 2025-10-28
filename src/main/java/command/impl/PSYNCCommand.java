package command.impl;

import java.io.BufferedWriter;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import replication.ReplicationManager;

public class PSYNCCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public PSYNCCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            String masterID = replicationManager.getMasterNode().getReplicationId();
            clientOutput.write(RESPSerializer.simpleString("FULLRESYNC "+masterID+" 0"));
            clientOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'PSYNC' command");
        }
    }

}
