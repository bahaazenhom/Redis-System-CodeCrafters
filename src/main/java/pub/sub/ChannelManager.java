package pub.sub;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {

    private final ConcurrentHashMap<String, List<String>> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HashSet<String>> channelSubscribers = new ConcurrentHashMap<>();
    private static ChannelManager instance = new ChannelManager();

    private ChannelManager() {
    }

    public static ChannelManager getInstance() {
        if (instance == null) {
            instance = new ChannelManager();
        }
        return instance;
    }

    public void subscribe(String channelName, String subscriberId) {
        channels.computeIfAbsent(channelName, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(subscriberId);

        channelSubscribers.computeIfAbsent(subscriberId, k -> new HashSet<>()).add(channelName);
    }

    public int getChannelsCount(String subscriberId) {
        HashSet<String> subscribedChannels = channelSubscribers.get(subscriberId);
        return subscribedChannels != null ? subscribedChannels.size() : 0;
    }

    public int getSubscribersCount(String channelName) {
        List<String> subscribers = channels.get(channelName);
        return subscribers != null ? subscribers.size() : 0;
    }

    public ConcurrentHashMap<String, List<String>> getChannels() {
        return channels;
    }

    public boolean isInSubscribeMode(String subscriberId) {
        return channelSubscribers.containsKey(subscriberId);
    }

    public boolean isSubscribeModeCommand(String commandName) {
        return commandName.equalsIgnoreCase("SUBSCRIBE") ||
                commandName.equalsIgnoreCase("PSUBSCRIBE") ||
                commandName.equalsIgnoreCase("UNSUBSCRIBE") ||
                commandName.equalsIgnoreCase("PUNSUBSCRIBE") ||
                commandName.equalsIgnoreCase("PING") ||
                commandName.equalsIgnoreCase("QUIT");
    }
}
