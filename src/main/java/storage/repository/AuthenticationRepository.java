package storage.repository;

import domain.values.UserProperties;

public interface AuthenticationRepository {
    String getCurrentUser();
    UserProperties getUserProperties(String currentUser);

    boolean addUserPassword(String userName, String password);

    boolean checkUserCredentials(String userName, String password);
}
