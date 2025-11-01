package command.impl;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import command.CommandStrategy;
import command.ResponseWriter.ClientConnection;
import protocol.RESPSerializer;
import replication.ReplicationManager;
import server.SlaveAckHandler;
import util.AppLogger;

public class PSYNCCommand implements CommandStrategy {
    private static final Logger log = AppLogger.getLogger(PSYNCCommand.class);
    private final ReplicationManager replicationManager;

    public PSYNCCommand(ReplicationManager replicationManager) {
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
            log.fine("Decoded RDB data: " + Arrays.toString(rdbData));

            // Write header and binary RDB data
            String header = "$" + rdbData.length + "\r\n";
            clientConnection.write(header);
            clientConnection.writeBytes(rdbData);
            clientConnection.flush();

            log.info("Sent RDB file (" + rdbData.length + " bytes)");

            Integer slavePort = replicationManager.getSlaveIdForConnection(clientConnection);
            log.info("Replica handshake complete. Registered listening port: "
                    + (slavePort != null ? slavePort : "unknown"));
            
            // Mark connection as handed over to SlaveAckHandler
            // ClientHandler should stop reading after PSYNC completes
            clientConnection.markHandoverToSlaveAckHandler();
            log.info("Marked connection for handover to SlaveAckHandler");
            
            log.info("Starting SlaveAckHandler thread for port " + slavePort 
                    + " using connection " + clientConnection);
            new Thread(() -> {
                log.info("SlaveAckHandler thread STARTED for port " + slavePort);
                new SlaveAckHandler(clientConnection, replicationManager.getMasterNode().getCommandExecuter()).run();
                log.info("SlaveAckHandler thread ENDED for port " + slavePort);
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
