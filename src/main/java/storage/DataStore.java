package storage;

import storage.operations.ChannelOperations;
import storage.operations.CommonOperations;
import storage.operations.ListOperations;
import storage.operations.StreamOperations;
import storage.operations.StringOperations;

public interface DataStore extends 
    CommonOperations,
    StringOperations, 
    ListOperations, 
    StreamOperations,
    ChannelOperations {
}
