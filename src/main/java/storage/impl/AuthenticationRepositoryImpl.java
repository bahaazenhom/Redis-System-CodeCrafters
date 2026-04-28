package storage.impl;

import domain.RedisValue;
import domain.values.UserProperties;
import server.connection.entity.ClientConnection;
import storage.repository.AuthenticationRepository;
import util.AppLogger;
import util.SHA256Util;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AuthenticationRepositoryImpl implements AuthenticationRepository {

    private final Map<String, RedisValue> store;
    Logger logger = AppLogger.getLogger(AuthenticationRepositoryImpl.class);


    public AuthenticationRepositoryImpl(Map<String, RedisValue> store) {
        this.store = store;
        populateDefaultUser();
    }

    @Override
    public String getCurrentUser() {
        return ClientConnection.getInstance().getUsername();
    }

    @Override
    public UserProperties getUserProperties(String currentUser) {
        return (UserProperties) store.get(currentUser);
    }

    @Override
    public boolean setUserPassword(String userName, String password) {

        // this user is not authorized to make changes to another user data
        if(!ClientConnection.getInstance().getUsername().equals(userName))return false;


        // Ensuring that "nopass" flag is removed, as we are adding a password to this user now.
        List<String> flags = getUserProperties(userName).getValue().get("flags");
        if (!flags.isEmpty() && flags.get(0).equals("nopass")) flags.remove(0);

        String sha256Password = SHA256Util.hashToHex(password.substring(1));
        getUserProperties(userName).getValue().get("passwords").add(sha256Password);

        ClientConnection clientConnection = ClientConnection.getInstance();
        clientConnection.setUserName(userName);
        clientConnection.setUserPassword(password.substring(1));

        return true;

    }

    @Override
    public boolean authenticateUser(String userName, String password) {
        UserProperties userProperties = getUserProperties(userName);
        if(userProperties == null){
            populateUserWithPassword(userName, password);
            return true;
        }
        if(userProperties.getValue().get("flags").contains("nopass")){
            return true; // No password required for this user
        }
        List<String> passwords = userProperties.getValue().get("passwords");
        ClientConnection clientConnection = ClientConnection.getInstance();
        logger.info("current client: "+ clientConnection.toString());
        for(String storedPassword:passwords){
            if(storedPassword.equals(SHA256Util.hashToHex(password))){
                clientConnection.setUserPassword(password);
                clientConnection.setUserName(userName);
                return true;
            }
        }
        return false;
    }

    private void populateDefaultUser(){
        if(store.containsKey("default"))return;
        UserProperties userProperties = new UserProperties();
        // Default Flags Population
        userProperties.getValue().put("flags", new ArrayList<>());
        userProperties.getValue().get("flags").add("nopass");

        // Default Passwords Population
        userProperties.getValue().put("passwords", new ArrayList<>());
        store.put("default", userProperties);
    }
    private void populateUserWithPassword(String userName, String password){
        if(store.containsKey(userName))return;
        UserProperties userProperties = new UserProperties();
        // Default Flags Population
        userProperties.getValue().put("flags", new ArrayList<>());

        // Default Passwords Population
        userProperties.getValue().put("passwords", new ArrayList<>()).add(SHA256Util.hashToHex(password));

        store.put(userName, userProperties);
        ClientConnection.getInstance().setUserName(userName);
    }
}
