
class PartyVote implements Comparable<PartyVote>{
    private int score;
    private String name;

    public PartyVote(int score, String name) {
        this.score = score;
        this.name = name;
    }

    public int getScore(){
        return score;
    }

    public String getName(){
        return name;
    }

    public void setScore(int score){
        this.score = score;
    }

    public void setName(String name){
        this.name = name;
    }

    @Override
    public int compareTo(PartyVote o) {
        return score < o.score ? -1 : score > o.score ? 1 : 0;
    }
}
