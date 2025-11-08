package storage.operations;


public interface ChannelOperations {
    void subscribe(String channelName, String subscriberId);

    int getChannelsCount(String subscriberId);

    int getSubscribersCount(String channelName);
}
