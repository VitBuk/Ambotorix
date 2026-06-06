package vitbuk.com.Ambotorix.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.scipts.LeaderScraper;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

@Service
public class DataUpdateService {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateService.class);
    private static final String BBG_INDEX_URL = "https://civ6bbg.github.io/index.html";

    private final LeaderService leaderService;
    private final NotificationService notificationService;

    @Value("${data.dir:src/main/resources}")
    private String dataDir;

    public DataUpdateService(LeaderService leaderService, NotificationService notificationService) {
        this.leaderService = leaderService;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void checkOnStartup() {
        log.info("Performing BBG version check on startup...");
        try {
            String latestVersion = fetchLatestVersion();
            String currentVersion = readCurrentVersion();
            if (isVersionNewer(currentVersion, latestVersion)) {
                log.info("BBG {} detected on startup (current: {}), updating...", latestVersion, currentVersion);
                runUpdate(latestVersion);
            } else {
                log.info("BBG data is up to date ({})", currentVersion);
            }
        } catch (Exception e) {
            log.error("Startup version check failed: {}", e.getMessage(), e);
            notificationService.notifyError("Startup BBG check failed", e.getMessage());
        }
    }

    @Scheduled(cron = "${data.update.cron:0 0 3 * * *}")
    public void scheduledCheck() {
        log.info("Running scheduled BBG version check...");
        checkAndUpdate();
    }

    public String checkAndUpdate() {
        try {
            String latestVersion = fetchLatestVersion();
            String currentVersion = readCurrentVersion();
            if (!isVersionNewer(currentVersion, latestVersion)) {
                return "Already on BBG " + currentVersion + " — no update needed.";
            }
            runUpdate(latestVersion);
            return "Updated to BBG " + latestVersion + " — leaders reloaded.";
        } catch (Exception e) {
            String msg = "Data update failed: " + e.getMessage();
            log.error(msg, e);
            notificationService.notifyError("BBG data update failed", e.getMessage());
            return msg;
        }
    }

    private void runUpdate(String version) throws Exception {
        Map<String, String> shortNameMap = loadShortNames();
        var leaders = LeaderScraper.scrapeLeaders(version, shortNameMap);
        saveLeadersJson(leaders);
        saveShortNames(shortNameMap);
        saveCurrentVersion(version);
        leaderService.reload();
        log.info("BBG {} update complete: {} leaders loaded", version, leaders.size());
    }

    static String fetchLatestVersion() throws IOException {
        Document doc = Jsoup.connect(BBG_INDEX_URL).timeout(30_000).get();
        return doc.select("a[href]").stream()
                .map(e -> e.attr("href"))
                .filter(href -> href.matches(".*leaders_[0-9]+\\.[0-9]+\\.html"))
                .map(href -> {
                    Matcher m = Pattern.compile("leaders_([0-9]+\\.[0-9]+)\\.html").matcher(href);
                    return m.find() ? m.group(1) : null;
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(Double::parseDouble))
                .orElseThrow(() -> new IOException("Could not detect BBG version from " + BBG_INDEX_URL));
    }

    public static boolean isVersionNewer(String current, String latest) {
        if (current == null || current.isBlank()) return true;
        try {
            return Double.parseDouble(latest) > Double.parseDouble(current);
        } catch (NumberFormatException e) {
            return !current.equals(latest);
        }
    }

    private String readCurrentVersion() {
        Path path = Path.of(dataDir, "bbg_version.txt");
        if (!Files.exists(path)) return "";
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private void saveCurrentVersion(String version) throws IOException {
        Files.writeString(Path.of(dataDir, "bbg_version.txt"), version, StandardCharsets.UTF_8);
    }

    private Map<String, String> loadShortNames() throws IOException {
        Path path = Path.of(dataDir, "leader_shortnames.json");
        if (!Files.exists(path)) return new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = new Gson().fromJson(reader, mapType);
            return map != null ? new LinkedHashMap<>(map) : new LinkedHashMap<>();
        }
    }

    private void saveShortNames(Map<String, String> shortNames) throws IOException {
        Path path = Path.of(dataDir, "leader_shortnames.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(shortNames, writer);
        }
    }

    private void saveLeadersJson(List<?> leaders) throws IOException {
        Path path = Path.of(dataDir, "civ6_leaders.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(leaders, writer);
        }
    }
}
