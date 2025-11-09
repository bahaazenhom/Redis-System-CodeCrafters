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
        int scoreComparison = Double.compare(this.score, o.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        return this.name.compareTo(o.name);
    }

}
