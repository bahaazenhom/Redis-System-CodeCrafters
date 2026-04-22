package storage.repository;

import domain.values.UserProperties;

public interface AuthenticationRepository {
    String getCurrentUser();
    UserProperties getUserProperties(String currentUser);
    boolean setUserPassword(String userName, String password);

    boolean authenticateUser(String userName, String password);
}
