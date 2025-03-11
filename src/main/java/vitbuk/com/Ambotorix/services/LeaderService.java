package vitbuk.com.Ambotorix.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.Constants;
import vitbuk.com.Ambotorix.entities.Leader;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

@Service
public class LeaderService {
    private List<Leader> leaders;

    public LeaderService(List<Leader> leaders) {
        this.leaders = loadLeaders();
    }

    private List<Leader> loadLeaders() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(Constants.LEADERS_JSON_PATH)) {
            Type listType = new TypeToken<List<Leader>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
