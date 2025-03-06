package vitbuk.com.Ambotorix;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.entities.Leader;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

@Component
public class AmbotorixService {
    private final TelegramClient telegramClient;
    private final String LEADERS_PATH = Constants.LEADERS_JSON_PATH;

    public AmbotorixService() {
        this.telegramClient = new OkHttpTelegramClient(Constants.BOT_TOKEN);
    }

    public List<Leader> getAllLeaders(String leadersPath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(leadersPath)) {
            Type listType = new TypeToken<List<Leader>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
