package command.impl.sub.pub;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import pub.sub.ChannelManager;
import server.connection.ClientConnection;

public class UnsubscribeCommand implements CommandStrategy {
    private ChannelManager channelManager;

    public UnsubscribeCommand() {
        this.channelManager = ChannelManager.getInstance();
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String subscriberId = clientOutput.getClientId();
            List<String> channels = arguments;
            int remainingChannels = channelManager.unsubscribe(subscriberId, channels);

            List<String> response = List.of("unsubscribe");
            response.addAll(channels);
            response.add(String.valueOf(remainingChannels));

            clientOutput.write(RESPSerializer.arraySubCommand(response));
            clientOutput.flush();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 1)
            throw new IllegalArgumentException("UNSUBSCRIBE command requires at least one argument.");
    }

}
