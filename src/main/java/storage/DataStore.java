package storage;

import storage.repository.CommonRepository;
import storage.repository.StringRepository;
import storage.repository.ListRepository;
import storage.repository.StreamRepository;
import storage.repository.SortedSetRepository;

public interface DataStore extends 
    CommonRepository,
    StringRepository, 
    ListRepository, 
    StreamRepository,
    SortedSetRepository {
}
