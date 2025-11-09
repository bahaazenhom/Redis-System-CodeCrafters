package storage.types;

public class Member implements Comparable<Member> {
    private final String name;
    private final double score;

    public Member(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Member o) {
        int nameComparison = this.name.compareTo(o.name);
        if (nameComparison == 0) {
            return nameComparison;
        }
        int scoreComparison = Double.compare(this.score, o.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        return this.name.compareTo(o.name) * -1;
    }

    @Override
    public String toString() {
        return "Member [name=" + name + ", score=" + score + "]";
    }

}
