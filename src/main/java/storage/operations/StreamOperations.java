package storage.operations;

import java.util.HashMap;
import java.util.List;

import storage.exception.InvalidStreamEntryException;

public interface StreamOperations {
    
    String xadd(String streamKey, String entryID, HashMap<String, String> entryValues) 
        throws InvalidStreamEntryException;

    List<List<Object>> XRANGE(String streamKey, String startEntryId, String endEntryId);
}
