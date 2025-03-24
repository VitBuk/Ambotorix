package vitbuk.com.Ambotorix.entities;

import java.util.List;

public class Player {
    String userName;
    List<Leader> picks;
    List<Leader> bans;

    public Player(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public void ban(Leader leader) {
        bans.add(leader);
    }
}
