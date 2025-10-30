package command.impl;

import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;

public class ListeningPortCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public ListeningPortCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            int listeningPort = Integer.parseInt(arguments.get(0));

            if (replicationManager.getMasterNode() != null) {
                replicationManager.registerSlaveConnection(listeningPort, clientOutput);
            }

            clientOutput.write(RESPSerializer.simpleString("OK"));
            clientOutput.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'LISTENING-PORT' command");
        }

        try {
            Integer.parseInt(arguments.get(0));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Argument for 'LISTENING-PORT' command must be a number");
        }
    }

}
