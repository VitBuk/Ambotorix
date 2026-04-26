package vitbuk.com.Ambotorix.scipts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import vitbuk.com.Ambotorix.entities.Leader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LeaderScraper {

    private static final Logger LOG = Logger.getLogger(LeaderScraper.class.getName());

    private static final String BASE_URL = "https://civ6bbg.github.io/index.html";
    private static final String IMAGE_BASE_URL = "https://civ6bbg.github.io/images/leaders/";
    private static final Path RESOURCES_DIR = Path.of("src", "main", "resources");
    private static final Path IMAGE_FOLDER  = RESOURCES_DIR.resolve("leaderImages");
    private static final Path OUTPUT_JSON   = RESOURCES_DIR.resolve("civ6_leaders.json");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
                                                            .connectTimeout(CONNECT_TIMEOUT)
                                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                                            .build();

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
    }

    public static List<Leader> scrapeLeaders() throws IOException {
        Document doc = Jsoup.connect(BASE_URL)
                            .timeout((int) REQUEST_TIMEOUT.toMillis())
                            .get();

        Elements leaderSections = doc.select("div.row[id]")
                                     .stream()
                                     .filter(e -> !e.select("h2.civ-name").isEmpty())
                                     .collect(Collectors.toCollection(Elements::new));

        List<Leader> leaders = new ArrayList<>();
        Files.createDirectories(IMAGE_FOLDER);

        for (Element section : leaderSections) {
            String fullName = section.attr("id");
            if (fullName.isBlank()) {
                LOG.warning("Skipping section without id");
                continue;
            }

            String shortName = " ";

            String encodedFullName = URLEncoder.encode(fullName, StandardCharsets.UTF_8)
                                               .replace("+", "%20");

            String safeFileName = fullName.replaceAll("[\\\\/:*?\"<>|]", "_");

            Path webpPath = IMAGE_FOLDER.resolve(safeFileName + ".webp");
            Path pngPath  = IMAGE_FOLDER.resolve(safeFileName + ".png");
            String imgUrl = IMAGE_BASE_URL + encodedFullName + ".webp";

            if (downloadImage(imgUrl, webpPath)) {
                if (convertWebPToPng(webpPath, pngPath)) {
                    Files.deleteIfExists(webpPath);
                }
            }

            String description = extractDescription(section);
            leaders.add(new Leader(fullName, shortName, description, pngPath.toString()));
        }

        return leaders;
    }

    private static String extractDescription(Element section) {
        StringBuilder description = new StringBuilder();
        Elements abilities = section.select("h3.civ-ability-name, p.civ-ability-desc.actual-text");
        for (Element ability : abilities) {
            String text = ability.text().trim();
            if (!text.isEmpty()) {
                description.append(text).append("\n");
            }
        }
        return description.toString().trim();
    }

    public static boolean downloadImage(String imageUrl, Path savePath) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(imageUrl))
                                             .timeout(REQUEST_TIMEOUT)
                                             .GET()
                                             .build();

            HttpResponse<Path> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(savePath)
            );

            if (response.statusCode() == 200) {
                LOG.info(() -> "Downloaded: " + savePath);
                return true;
            }

            LOG.warning(() -> "HTTP " + response.statusCode() + " for " + imageUrl);
            Files.deleteIfExists(savePath); // удалить пустой/частичный файл
            return false;

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to download: " + imageUrl, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Download interrupted: " + imageUrl, e);
            return false;
        }
    }

    public static boolean convertWebPToPng(Path webpPath, Path pngPath) {
        try (ImageInputStream input = ImageIO.createImageInputStream(webpPath.toFile())) {
            if (input == null) {
                LOG.warning(() -> "Cannot open image stream: " + webpPath);
                return false;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                LOG.warning("No suitable WebP reader found.");
                return false;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                BufferedImage image = reader.read(0);
                ImageIO.write(image, "png", pngPath.toFile());
                LOG.info(() -> "Converted to PNG: " + pngPath);
                return true;
            } finally {
                reader.dispose();
            }

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error converting WebP to PNG: " + webpPath, e);
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            List<Leader> leaders = scrapeLeaders();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (var writer = Files.newBufferedWriter(OUTPUT_JSON, StandardCharsets.UTF_8)) {
                gson.toJson(leaders, writer);
            }

            LOG.info(() -> "Saved " + leaders.size() + " leaders to " + OUTPUT_JSON);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Scraping failed", e);
            System.exit(1);
        }
    }
}