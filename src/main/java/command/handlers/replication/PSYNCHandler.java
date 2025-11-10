package command.handlers.replication;

import java.util.Base64;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.connection.ClientConnection;
import server.handler.SlaveAckHandler;

public class PSYNCHandler implements CommandStrategy {
    private final ReplicationManager replicationManager;

    public PSYNCHandler(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientConnection) {
        try {
            // Send FULLRESYNC response
            String masterID = replicationManager.getMasterNode().getId();
            clientConnection.write(RESPSerializer.simpleString("FULLRESYNC " + masterID + " 0"));
            clientConnection.flush(); // flush text

            // Send RDB file (empty RDB for simplicity)
            String emptyRdbBase64 = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
            byte[] rdbData = Base64.getDecoder().decode(emptyRdbBase64);

            // Write header and binary RDB data
            String header = "$" + rdbData.length + "\r\n";
            clientConnection.write(header);
            clientConnection.writeBytes(rdbData);
            clientConnection.flush();

            Integer slavePort = replicationManager.getSlaveIdForConnection(clientConnection);
            
            // Mark connection as handed over to SlaveAckHandler
            // ClientHandler should stop reading after PSYNC completes
            clientConnection.markHandoverToSlaveAckHandler();
            
            // Start thread to handle ACK responses from this replica
            new Thread(() -> {
                new SlaveAckHandler(clientConnection, replicationManager.getMasterNode().getCommandExecuter()).run();
            }, "SlaveAckHandler-" + slavePort).start();

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
