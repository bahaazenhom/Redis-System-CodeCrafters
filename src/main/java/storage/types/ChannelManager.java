package storage.types;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {

    private final ConcurrentHashMap<String, List<String>> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HashSet<String>> channelSubscriberCounts = new ConcurrentHashMap<>();

    public ChannelManager() {
    }

    public void subscribe(String channelName, String subscriberId) {
        channels.computeIfAbsent(channelName, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(subscriberId);

        channelSubscriberCounts.computeIfAbsent(subscriberId, k -> new HashSet<>()).add(channelName);
    }

    public int getSubscriberCount(String subscriberId) {
        HashSet<String> subscribedChannels = channelSubscriberCounts.get(subscriberId);
        return subscribedChannels != null ? subscribedChannels.size() : 0;
    }

    public ConcurrentHashMap<String, List<String>> getChannels() {
        return channels;
    }
}
