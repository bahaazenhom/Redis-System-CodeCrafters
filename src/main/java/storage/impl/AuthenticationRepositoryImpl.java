package storage.impl;

import domain.RedisValue;
import domain.values.UserProperties;
import server.connection.ClientConnection;
import storage.DataStore;
import storage.repository.AuthenticationRepository;

import java.sql.SQLOutput;
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
        UserProperties userProperties = new UserProperties();
        // Default Flags Population
        userProperties.getValue().put("flags", new ArrayList<>());
        userProperties.getValue().get("flags").add("nopass");

        // Default Passwords Population
        userProperties.getValue().put("passwords", new ArrayList<>());
        store.put("default", userProperties);
    }
}
