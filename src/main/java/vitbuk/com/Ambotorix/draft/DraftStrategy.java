package vitbuk.com.Ambotorix.draft;

import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public interface DraftStrategy {
    String getName();
    void execute(Lobby lobby, Long chatId, AmbotorixService service);
    default void onAllPicksIn(Lobby lobby, Long chatId, AmbotorixService service) {}
}
