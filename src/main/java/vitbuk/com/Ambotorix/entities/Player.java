package vitbuk.com.Ambotorix.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    String userName;
    List<Leader> picks;
    List<Leader> bans;

    public Player(String userName) {
        this.userName = userName;
        picks = new ArrayList<>();
        bans = new ArrayList<>();
    }

    public String getUserName() {
        return userName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return userName.equals(player.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName);
    }
}
