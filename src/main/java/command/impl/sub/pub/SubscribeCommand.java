package command.impl.sub.pub;

import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;

public class SubscribeCommand implements CommandStrategy {
    private final DataStore dataStore;

    public SubscribeCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            List<String> channels = arguments;
            String subscriberId = clientOutput.getClientId();
            for(String channel : channels) {
                dataStore.subscribe(subscriberId, channel);
                int subscriberChannelsCount = dataStore.getChannelsCount(subscriberId);

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
