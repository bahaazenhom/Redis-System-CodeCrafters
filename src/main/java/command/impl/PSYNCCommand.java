package command.impl;

import java.util.Base64;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;

public class PSYNCCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public PSYNCCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            // Send FULLRESYNC response
            String masterID = replicationManager.getMasterNode().getReplicationId();
            clientOutput.write(RESPSerializer.simpleString("FULLRESYNC "+masterID+" 0"));

            // Send RDB file (empty RDB for simplicity)
            String emptyRdbBase64 =
             "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
            byte[] rdbData = Base64.getDecoder().decode(emptyRdbBase64);
            String header = "$" + rdbData.length + "\r\n";
            // Write header and RDB data 
            clientOutput.write(header); // Write header (as string)
            clientOutput.flush();
            clientOutput.writeBytes(rdbData);
            clientOutput.write("\r\n");
            clientOutput.flush();

            // Register the slave connection for future command propagation
            replicationManager.getSlaveNodesSockets().add(clientOutput);

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
