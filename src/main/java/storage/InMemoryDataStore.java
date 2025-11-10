package storage;

import storage.DataStore;
import storage.impl.CommonRepositoryImpl;
import storage.impl.ListRepositoryImpl;
import storage.impl.SortedSetRepositoryImpl;
import storage.impl.StreamRepositoryImpl;
import storage.impl.StringRepositoryImpl;
import storage.concurrency.ListWaitRegistry;
import storage.concurrency.StreamWaitRegistry;
import domain.DataType;
import domain.RedisValue;
import storage.exception.InvalidStreamEntryException;
import domain.values.Member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {

    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    
    private final CommonRepositoryImpl commonRepository;
    private final StringRepositoryImpl stringRepository;
    private final ListRepositoryImpl listRepository;
    private final StreamRepositoryImpl streamRepository;
    private final SortedSetRepositoryImpl sortedSetRepository;

    public InMemoryDataStore() {
        ListWaitRegistry listWaitRegistry = new ListWaitRegistry();
        StreamWaitRegistry streamWaitRegistry = new StreamWaitRegistry();
        
        this.commonRepository = new CommonRepositoryImpl(store);
        this.stringRepository = new StringRepositoryImpl(store);
        this.listRepository = new ListRepositoryImpl(store, listWaitRegistry);
        this.streamRepository = new StreamRepositoryImpl(store, streamWaitRegistry);
        this.sortedSetRepository = new SortedSetRepositoryImpl(store);
    }

    // ============================================
    // DELEGATE TO COMMON REPOSITORY
    // ============================================

    @Override
    public void setValue(String key, RedisValue redisValue) {
        commonRepository.setValue(key, redisValue);
    }

    @Override
    public RedisValue getValue(String key) {
        return commonRepository.getValue(key);
    }

    @Override
    public boolean exists(String key) {
        return commonRepository.exists(key);
    }

    @Override
    public boolean delete(String key) {
        return commonRepository.delete(key);
    }

    @Override
    public void cleanup() {
        commonRepository.cleanup();
    }

    @Override
    public long getTTL(String key) {
        return commonRepository.getTTL(key);
    }

    @Override
    public DataType getType(String key) {
        return commonRepository.getType(key);
    }

    @Override
    public boolean isType(String key, DataType dataType) {
        return commonRepository.isType(key, dataType);
    }

    @Override
    public Set<String> getAllKeys() {
        return commonRepository.getAllKeys();
    }

    // ============================================
    // DELEGATE TO STRING REPOSITORY
    // ============================================

    @Override
    public long incr(String key) {
        return stringRepository.incr(key);
    }

    // ============================================
    // DELEGATE TO LIST REPOSITORY
    // ============================================

    @Override
    public long rpush(String key, List<String> values) {
        return listRepository.rpush(key, values);
    }

    @Override
    public long lpush(String key, List<String> values) {
        return listRepository.lpush(key, values);
    }

    @Override
    public String lpop(String key) {
        return listRepository.lpop(key);
    }

    @Override
    public List<String> lpop(String key, Long count) {
        return listRepository.lpop(key, count);
    }

    @Override
    public String BLPOP(String key, double timestamp) throws InterruptedException {
        return listRepository.BLPOP(key, timestamp);
    }

    // ============================================
    // DELEGATE TO STREAM REPOSITORY
    // ============================================

    @Override
    public String xadd(String streamKey, String entryID, HashMap<String, String> entryValues)
            throws InvalidStreamEntryException {
        return streamRepository.xadd(streamKey, entryID, entryValues);
    }

    @Override
    public List<List<Object>> XRANGE(String streamKey, String startEntryId, String endEntryId, boolean inclusion) {
        return streamRepository.XRANGE(streamKey, startEntryId, endEntryId, inclusion);
    }

    @Override
    public List<List<Object>> XREAD(List<String> streamsKeys, List<String> streamsStartEntriesIDs, boolean block,
            double timeoutSeconds) throws InterruptedException {
        return streamRepository.XREAD(streamsKeys, streamsStartEntriesIDs, block, timeoutSeconds);
    }

    // ============================================
    // DELEGATE TO SORTED SET REPOSITORY
    // ============================================

    @Override
    public int zadd(String key, List<Member> members) {
        return sortedSetRepository.zadd(key, members);
    }

    @Override
    public Integer zrank(String key, String memberName) {
        return sortedSetRepository.zrank(key, memberName);
    }

    @Override
    public List<String> zrange(String key, int start, int end) {
        return sortedSetRepository.zrange(key, start, end);
    }

    @Override
    public int zcard(String key) {
        return sortedSetRepository.zcard(key);
    }

    @Override
    public double zscore(String key, String memberName) {
        return sortedSetRepository.zscore(key, memberName);
    }

    @Override
    public int zrem(String key, String memberName) {
        return sortedSetRepository.zrem(key, memberName);
    }
}
