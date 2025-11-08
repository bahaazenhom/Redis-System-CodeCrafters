package storage.operations;


public interface ChannelOperations {
    void subscribe(String channelName, String subscriberId);

    int getSubscriberCount(String subscriberId);
}
