package storage.impl;

import domain.RedisValue;
import domain.values.UserProperties;
import server.connection.ClientConnection;
import storage.repository.AuthenticationRepository;
import util.SHA256Util;

import java.util.ArrayList;
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

    @Override
    public boolean addUserPassword(String userName, String password) {
        // Ensuring that "nopass" flag is removed, as we are adding a password to this user now.
        if(getUserProperties(userName) == null)return false;
        List<String> flags = getUserProperties(userName).getValue().get("flags");
        if (!flags.isEmpty() && flags.get(0).equals("nopass")) flags.remove(0);

        String sha256Password = SHA256Util.hashToHex(password.substring(1));
        getUserProperties(userName).getValue().get("passwords").add(sha256Password);
        return true;
    }

    @Override
    public boolean checkUserCredentials(String userName, String password) {
        UserProperties userProperties = getUserProperties(userName);
        if(userProperties == null)return false;
        if(userProperties.getValue().get("flags").contains("nopass")){
            return true; // No password required for this user
        }
        List<String> passwords = userProperties.getValue().get("passwords");
        for(String storedPassword:passwords){
            if(storedPassword.equals(SHA256Util.hashToHex(password))){
                return true;
            }
        }
        return false;
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
