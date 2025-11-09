package storage.types;

import java.util.TreeSet;

import storage.core.DataType;
import storage.core.RedisValue;

public class SortedSetValue extends RedisValue {
    private final TreeSet<Member> members;

    public SortedSetValue(DataType type) {
        super(type);
        this.members = new TreeSet<>();
    }

    public void addMember(Member member) {
        members.add(member);
    }

    public void removeMember(Member member) {
        members.remove(member);
    }

    public Member getMember(String name) {
        for (Member member : members) {
            if (member.getName().equals(name)) {
                return member;
            }
        }
        return null;
    }

    public int getRank(Member member) {
        int rank = members.headSet(member).size();
        return rank;
    }

    @Override
    public Object getValue() {
        return members;
    }

    public TreeSet<Member> getMembers() {
        return members;
    }

}
