package vitbuk.com.Ambotorix.scipts;

import vitbuk.com.Ambotorix.entities.Leader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;

public class LeaderScraper {
    private static final String BASE_URL = "https://calcalciffer.github.io/en_US/leaders_6.2.html";
    private static final String IMAGE_FOLDER = "leaderImages";

    public static List<Leader> scrapeLeaders() throws IOException {
        Document doc = Jsoup.connect(BASE_URL).get();
        Elements leaderSections = doc.select("div.leaders-data div.row");
        List<Leader> leaders = new ArrayList<>();
        new File(IMAGE_FOLDER).mkdirs(); // Ensure folder exists

        for (Element section : leaderSections) {
            String fullName = section.attr("id");
            String shortName = " ";
            String encodedFullName = URLEncoder.encode(fullName, StandardCharsets.UTF_8).replace("+", "%20");
            String webpPath = IMAGE_FOLDER + "/" + fullName + ".webp";
            String pngPath = IMAGE_FOLDER + "/" + fullName + ".png";
            String imgUrl = "https://calcalciffer.github.io/images/leaders/" + encodedFullName + ".webp";

            if (downloadImage(imgUrl, webpPath)) {
                convertWebPToPng(webpPath, pngPath);
                new File(webpPath).delete(); // Remove the original .webp file
            }

            StringBuilder description = new StringBuilder();
            Elements abilities = section.select("h3.civ-ability-name, p.civ-ability-desc");
            for (Element ability : abilities) {
                description.append(ability.text()).append("\n");
            }

            leaders.add(new Leader(fullName, shortName, description.toString().trim(), pngPath));
        }
        return leaders;
    }

    public static boolean downloadImage(String imageUrl, String savePath) {
        try (InputStream in = new URL(imageUrl).openStream(); FileOutputStream out = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("Downloaded: " + savePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to download: " + imageUrl);
            return false;
        }
    }

    public static void convertWebPToPng(String webpPath, String pngPath) {
        try {
            IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
            File webpFile = new File(webpPath);
            try (ImageInputStream input = ImageIO.createImageInputStream(webpFile)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(input);
                    BufferedImage image = reader.read(0);
                    ImageIO.write(image, "png", new File(pngPath));
                    System.out.println("Converted to PNG: " + pngPath);
                } else {
                    System.err.println("No suitable WebP reader found.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error converting WebP to PNG: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            List<Leader> leaders = scrapeLeaders();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter("civ6_leaders.json")) {
                gson.toJson(leaders, writer);
            }
            System.out.println("Data saved to civ6_leaders.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}