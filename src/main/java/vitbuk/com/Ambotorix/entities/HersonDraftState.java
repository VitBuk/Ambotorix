package vitbuk.com.Ambotorix.entities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-lobby bookkeeping for a Herson draft. Lives on the {@link Lobby} (in memory, like the rest of
 * lobby state) and is created when the draft starts.
 *
 * <p>Players submit four ranked picks over DM (free text). Each player walks a small state machine —
 * {@link Stage} — until they have submitted; the resolution algorithm then collapses everyone's
 * ranked lists into a unique assignment, occasionally pausing for a coin-flip re-pick (which puts a
 * player back into {@link Stage#AWAITING_REPICK}).
 */
public class HersonDraftState {

    /** Where a single player is in the DM submission flow. */
    public enum Stage {
        /** Expecting the player's message with their four ranked picks. */
        AWAITING_PICKS,
        /** Fuzzy matching corrected something — expecting the player to confirm (button) or re-send. */
        AWAITING_CONFIRM,
        /** Lost a coin flip — expecting one civ name to re-pick from the remaining pool. */
        AWAITING_REPICK,
        /** Player's pick(s) are final. */
        SUBMITTED
    }

    // username -> current stage
    private final Map<String, Stage> stages = new LinkedHashMap<>();
    // username -> their four ranked leaders (priority order), once committed
    private final Map<String, List<Leader>> rankedPicks = new LinkedHashMap<>();
    // username -> matched-but-unconfirmed picks awaiting a Confirm tap
    private final Map<String, List<Leader>> pendingConfirm = new LinkedHashMap<>();

    // Resolution working state — persists across coin-flip suspensions.
    private final Set<Leader> banned = new LinkedHashSet<>();      // contested civs removed from the pool
    private final Map<String, Leader> assigned = new LinkedHashMap<>(); // username -> final civ (coin-flip winners / re-pickers)

    public void init(List<Player> players) {
        for (Player p : players) {
            stages.put(p.getUserName(), Stage.AWAITING_PICKS);
        }
    }

    public Stage getStage(String userName) { return stages.get(userName); }
    public void setStage(String userName, Stage stage) { stages.put(userName, stage); }

    public Map<String, List<Leader>> getRankedPicks() { return rankedPicks; }
    public Set<Leader> getBanned() { return banned; }
    public Map<String, Leader> getAssigned() { return assigned; }

    public List<Leader> getPendingConfirm(String userName) { return pendingConfirm.get(userName); }
    public void setPendingConfirm(String userName, List<Leader> picks) { pendingConfirm.put(userName, new ArrayList<>(picks)); }
    public void clearPendingConfirm(String userName) { pendingConfirm.remove(userName); }

    /** True while any player still owes the bot input (picks, a confirm, or a re-pick). */
    public boolean anyAwaitingInput() {
        return stages.values().stream().anyMatch(s -> s != Stage.SUBMITTED);
    }

    /** True once every player has committed their four ranked picks. */
    public boolean allRankedSubmitted(int playerCount) {
        return rankedPicks.size() >= playerCount;
    }

    public boolean anyAwaitingRepick() {
        return stages.values().stream().anyMatch(s -> s == Stage.AWAITING_REPICK);
    }
}
