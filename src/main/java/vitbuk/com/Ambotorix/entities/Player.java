package vitbuk.com.Ambotorix.entities;

import java.util.List;

public class Player {
    String name;
    List<Leader> picks;
    List<Leader> bans;

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Leader> getPicks() {
        return picks;
    }

    public void setPicks(List<Leader> picks) {
        this.picks = picks;
    }

    public List<Leader> getBans() {
        return bans;
    }

    public void setBans(List<Leader> bans) {
        this.bans = bans;
    }
}
