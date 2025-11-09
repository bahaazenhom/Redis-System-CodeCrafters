package pub.sub;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import protocol.RESPSerializer;
import server.connection.ClientConnection;

public class ChannelManager {

    private final ConcurrentHashMap<String, List<ClientConnection>> channels = new ConcurrentHashMap<>();
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

    public void subscribe(String channelName, ClientConnection subscriber) {
        channels.computeIfAbsent(channelName, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(subscriber);

        channelSubscribers.computeIfAbsent(subscriber.getClientId(), k -> new HashSet<>()).add(channelName);
    }

    public int getChannelsCount(String subscriberId) {
        HashSet<String> subscribedChannels = channelSubscribers.get(subscriberId);
        return subscribedChannels != null ? subscribedChannels.size() : 0;
    }

    public int getSubscribersCount(String channelName) {
        List<ClientConnection> subscribers = channels.get(channelName);
        return subscribers != null ? subscribers.size() : 0;
    }

    public ConcurrentHashMap<String, List<ClientConnection>> getChannels() {
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

    public int publishMessageToChannel(String channelName, String message) {
        List<ClientConnection> subscribers = channels.get(channelName);
        if (subscribers == null) {
            return 0;
        }

        for (ClientConnection subscriber : subscribers) {
            try{
                List<String> response = List.of("message", channelName, message);
                subscriber.write(RESPSerializer.array(response));
                subscriber.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("Sending message to subscriber " + subscriber.getClientId() + ": " + message);
        }
        return subscribers.size();
    }
}
