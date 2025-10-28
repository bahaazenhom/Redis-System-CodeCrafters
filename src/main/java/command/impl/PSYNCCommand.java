package command.impl;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import command.CommandStrategy;
import command.ResponseWriter.ResponseWriter;
import protocol.RESPSerializer;
import replication.ReplicationManager;

public class PSYNCCommand implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public PSYNCCommand(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ResponseWriter clientOutput) {
        try {
            String masterID = replicationManager.getMasterNode().getReplicationId();
            clientOutput.write(RESPSerializer.simpleString("FULLRESYNC "+masterID+" 0"));
            String emptyRdbBase64 =
             "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
            byte[] rdbData = Base64.getDecoder().decode(emptyRdbBase64);
            String header = "$" + rdbData.length + "\r\n";
            System.out.println("header is: " + header);
            System.out.println("binary data is: " + Arrays.toString(rdbData));
            clientOutput.write(header);
            clientOutput.writeBytes(rdbData);
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
