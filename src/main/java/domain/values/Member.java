package domain.values;

public class Member implements Comparable<Member> {
    private final String name;
    private final double score;

    public Member(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public Member(String name, double longitude, double latitude) {
        this.name = name;
        this.score = longitude + (latitude / 1000.0);
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Member o) {
        // First compare by score (for ordering in sorted set)
        int scoreComparison = Double.compare(this.score, o.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        // If scores are equal, compare by name (for consistent ordering)
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return "Member [name=" + name + ", score=" + score + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Member other = (Member) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
