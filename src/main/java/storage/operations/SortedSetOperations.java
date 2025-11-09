package storage.operations;

import java.util.List;

import storage.types.Member;

public interface SortedSetOperations {
    int zadd(String key, List<Member> members);

    Integer zrank(String key, String memberName);

    List<String> zrange(String key, int start, int end);

    int zcard(String key);

    double zscore(String key, String memberName);
}
