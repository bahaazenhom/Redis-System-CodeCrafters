package storage.repository;

import domain.values.UserProperties;

public interface AuthenticationRepository {
    String getCurrentUser();
    UserProperties getUserProperties(String currentUser);
}
