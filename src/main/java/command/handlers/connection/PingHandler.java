package command.handlers.connection;

import command.CommandStrategy;
import protocol.RESPSerializer;
import pub.sub.ChannelManager;
import replication.ReplicationManager;
import server.connection.ClientConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PingHandler implements CommandStrategy {
    private final ChannelManager channelManager = ChannelManager.getInstance();

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        // PING command accepts 0 or 1 arguments (optional message)
        if (arguments.size() > 1) {
            throw new IllegalArgumentException("wrong number of arguments for 'ping' command");
        }
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            if (ReplicationManager.isSlaveNode())
                return;

            if (channelManager.isInSubscribeMode(clientOutput.getClientId())) {
                List<String> response = new ArrayList<>();
                response.add("pong");
                response.add("");
                clientOutput.write(RESPSerializer.array(response));
            } else
                clientOutput.write(RESPSerializer.simpleString("PONG"));

            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
