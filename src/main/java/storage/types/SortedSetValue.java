package storage.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import storage.core.DataType;
import storage.core.RedisValue;

public class SortedSetValue extends RedisValue {
    private final TreeSet<Member> members;
    // Map to quickly find members by name
    private final HashMap<String, Member> membersByName;

    public SortedSetValue(DataType type) {
        super(type);
        this.members = new TreeSet<>();
        this.membersByName = new HashMap<>();
    }

    public void addMember(Member member) {
        // Remove old member with same name if exists
        Member oldMember = membersByName.get(member.getName());
        if (oldMember != null) {
            members.remove(oldMember);
        }
        // Add new member
        members.add(member);
        membersByName.put(member.getName(), member);
    }

    public void removeMember(Member member) {
        members.remove(member);
        membersByName.remove(member.getName());
    }

    public Member getMember(String name) {
        return membersByName.get(name);
    }

    public int getRank(Member member) {
        int rank = members.headSet(member).size();
        return rank;
    }

    public List<String> getRange(int start, int end) {
        List<String> rangeMembers = new ArrayList<>();
        int index = 0;
        if (members.isEmpty() || start > end) {
            return rangeMembers;
        }
        for (Member member : members) {
            if (index >= start && index <= end) {
                rangeMembers.add(member.getName());
            }
            if (index > end) {
                break;
            }
            index++;
        }
        return rangeMembers;
    }

    @Override
    public Object getValue() {
        return members;
    }

    public TreeSet<Member> getMembers() {
        return members;
    }

}
