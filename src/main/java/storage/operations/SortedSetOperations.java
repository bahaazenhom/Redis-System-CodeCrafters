package storage.operations;

import java.util.List;

import storage.types.Member;

public interface SortedSetOperations {
    int zadd(String key, List<Member> members);

    int zrank(String key, String memberName);

    void zrem(String key, String memberName);

    Double zscore(String key, String memberName);
}
