package domain.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import domain.DataType;
import domain.RedisValue;

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

    public double getScore(String name) {
        Member member = membersByName.get(name);
        if (member != null) {
            return member.getScore();
        }
        return -1; // or throw exception if member not found
    }

    public int removeMember(String name) {
        Member member = membersByName.get(name);
        if (member != null) {
            members.remove(member);
            membersByName.remove(name);
            return 1; // Indicate that a member was removed
        }
        return 0; // Indicate that no member was removed
    }

    public List<String> getRange(int start, int end) {
        if (start < 0)
            start = members.size() + start;
        if (end < 0)
            end = members.size() + end;
        if (start < 0)
            start = 0;
        if (end < 0)
            end = 0;

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

    public int getSize() {
        return members.size();
    }

    @Override
    public Object getValue() {
        return members;
    }

    public TreeSet<Member> getMembers() {
        return members;
    }

}
