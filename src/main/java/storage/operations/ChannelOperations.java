package storage.operations;

import server.connection.ClientConnection;

public interface ChannelOperations {
    void subscribe(String channel, ClientConnection clientConnection);

    int getChannelsCount(String subscriberId);

    int getSubscribersCount(String channelName);

    int publishMessageToChannel(String channelName, String message);

}
