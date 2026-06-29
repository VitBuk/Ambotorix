package vitbuk.com.Ambotorix.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    private String userName;
    private Long userId;
    private List<Leader> picks;
    private List<Leader> bans;
    private List<String> priorityPicks = new ArrayList<>();
    private Long dmPickMessageId;

    public Player(String userName, Long userId) {
        this.userName = userName;
        this.userId = userId;
        this.picks = new ArrayList<>();
        this.bans = new ArrayList<>();
    }

    public String getUserName() { return userName; }
    public Long getUserId() { return userId; }
    public List<Leader> getPicks() { return picks; }
    public void setPicks(List<Leader> picks) { this.picks = picks; }
    public List<Leader> getBans() { return bans; }
    public void setBans(List<Leader> bans) { this.bans = bans; }
    public void ban(Leader leader) { bans.add(leader); }

    public List<String> getPriorityPicks() { return priorityPicks; }
    public void setPriorityPicks(List<String> priorityPicks) { this.priorityPicks = priorityPicks; }
    public Long getDmPickMessageId() { return dmPickMessageId; }
    public void setDmPickMessageId(Long dmPickMessageId) { this.dmPickMessageId = dmPickMessageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return userName.equals(player.userName);
    }

    @Override
    public int hashCode() { return Objects.hash(userName); }
}
