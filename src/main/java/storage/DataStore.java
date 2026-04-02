package storage;

import storage.repository.*;

public interface DataStore extends
        CommonRepository,
        StringRepository,
        ListRepository,
        StreamRepository,
        SortedSetRepository,
        AuthenticationRepository {
}
