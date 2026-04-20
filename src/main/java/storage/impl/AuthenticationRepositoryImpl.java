package storage.impl;

import domain.RedisValue;
import domain.values.UserProperties;
import server.connection.ClientConnection;
import storage.DataStore;
import storage.repository.AuthenticationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationRepositoryImpl implements AuthenticationRepository {

    private final ClientConnection clientConnection;
    private final Map<String, RedisValue> store;

    public AuthenticationRepositoryImpl(ClientConnection clientConnection, Map<String, RedisValue> store) {
        this.clientConnection = clientConnection;
        this.store = store;
        populateDefaultUser();
    }

    @Override
    public String getCurrentUser() {
        return clientConnection.getUsername();
    }

    @Override
    public UserProperties getUserProperties(String currentUser) {
        return (UserProperties) store.get(currentUser);
    }

    private void populateDefaultUser(){
        store.put("default",new UserProperties());
        ((Map<String, List<String>>)((UserProperties)store.get("default")).getValue()).put("flags",new ArrayList<>());
    }
}
