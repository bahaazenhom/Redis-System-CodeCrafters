package command.handlers.pubsub;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import pub.sub.ChannelManager;
import server.connection.ClientConnection;

public class PublishHandler implements CommandStrategy {
    private ChannelManager channelManager;

    public PublishHandler() {
        this.channelManager = ChannelManager.getInstance();
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String channelName = arguments.get(0);
            String messageContent = arguments.get(1);
            int receiversCount = channelManager.publishMessageToChannel(channelName, messageContent);
            clientOutput.write(RESPSerializer.integer(receiversCount));
            clientOutput.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("PUBLISH command requires at least two arguments.");
        }
    }

}
