package command.handlers.pubsub;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import pub.sub.ChannelManager;
import server.connection.ClientConnection;

public class SubscribeHandler implements CommandStrategy {
    private ChannelManager channelManager;

    public SubscribeHandler() {
        this.channelManager = ChannelManager.getInstance();
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            List<String> channels = arguments;
            String subscriberId = clientOutput.getClientId();
            for (String channel : channels) {
                channelManager.subscribe(channel, clientOutput);
                int subscriberChannelsCount = channelManager.getChannelsCount(subscriberId);

                List<String> response = List.of("subscribe", channel, String.valueOf(subscriberChannelsCount));
                clientOutput.write(RESPSerializer.arraySubCommand(response));
                clientOutput.flush();

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 1)
            throw new IllegalArgumentException("SUBSCRIBE command requires at least one argument.");
    }

}
