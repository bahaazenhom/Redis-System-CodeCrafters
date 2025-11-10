package storage.impl;

import domain.DataType;
import domain.RedisValue;
import domain.values.Member;
import domain.values.SortedSetValue;
import storage.repository.SortedSetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SortedSetRepositoryImpl implements SortedSetRepository {

    private final Map<String, RedisValue> store;

    public SortedSetRepositoryImpl(Map<String, RedisValue> store) {
        this.store = store;
    }

    @Override
    public int zadd(String key, List<Member> members) {
        SortedSetValue sortedSetValue = getOrCreateSortedSet(key);
        int sizeBefore = sortedSetValue.getMembers().size();

        for (Member member : members) {
            sortedSetValue.addMember(member);
        }

        return sortedSetValue.getMembers().size() - sizeBefore;
    }

    @Override
    public Integer zrank(String key, String memberName) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof SortedSetValue)) {
            return -1;
        }
        SortedSetValue sortedSetValue = (SortedSetValue) redisValue;
        Member member = sortedSetValue.getMember(memberName);
        if (member == null) {
            return -1;
        }
        return sortedSetValue.getRank(member);
    }

    @Override
    public List<String> zrange(String key, int start, int end) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof SortedSetValue)) {
            return new ArrayList<>();
        }
        SortedSetValue sortedSetValue = (SortedSetValue) redisValue;
        return sortedSetValue.getRange(start, end);
    }

    @Override
    public int zcard(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof SortedSetValue)) {
            return 0;
        }
        SortedSetValue sortedSetValue = (SortedSetValue) redisValue;
        return sortedSetValue.getSize();
    }

    @Override
    public double zscore(String key, String memberName) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof SortedSetValue)) {
            return -1;
        }
        SortedSetValue sortedSetValue = (SortedSetValue) redisValue;
        return sortedSetValue.getScore(memberName);
    }

    @Override
    public int zrem(String key, String memberName) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof SortedSetValue)) {
            return 0;
        }
        SortedSetValue sortedSetValue = (SortedSetValue) redisValue;
        return sortedSetValue.removeMember(memberName);
    }

    private SortedSetValue getOrCreateSortedSet(String key) {
        RedisValue value = store.computeIfAbsent(key, k -> new SortedSetValue(DataType.SORTED_SET));
        if (!(value instanceof SortedSetValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return (SortedSetValue) value;
    }
}
