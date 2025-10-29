package command.impl;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
public class INFOCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public INFOCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            if (arguments.get(0).equals("replication")) {
                String info;
                if(replicationManager.getMasterNode()!=null){
                    info = replicationManager.getMasterNode().getInfo();
                }
                else info = replicationManager.getCurrentSlaveInfo();
                clientOutput.write(RESPSerializer.bulkString(info));
                clientOutput.flush();
            }
        } catch (NumberFormatException e) {
            try {
                clientOutput.write("Invalid port number\n");
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        try {
            if (arguments.size() != 1) {
                throw new IllegalArgumentException("Wrong number of arguments for 'INFO' command");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid arguments for 'INFO' command");
        }
    }

}
