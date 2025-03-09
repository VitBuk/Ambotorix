package vitbuk.com.Ambotorix;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import vitbuk.com.Ambotorix.entities.Leader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PickImageGenerator {

    private static File generateLeaderPickImage(String playerName, List<Leader> leaders){
        int iconSize = 80;
        int padding = 10;
        int width = leaders.size() * (iconSize + padding);
        int height = iconSize + 40;

        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = finalImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Verdana", Font.BOLD, 14));

        int x = 0;
        for (Leader leader : leaders) {
            BufferedImage leaderIcon = null;
            try {
                leaderIcon = ImageIO.read(new File(leader.getPicPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Image scaledIcon = leaderIcon.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            g.drawImage(scaledIcon, x, 0, null);

            g.drawString(leader.getFullName(), x + 5, iconSize + 20);
            x += iconSize + padding;
        }

        g.dispose();

        File outputFile = new File("leader_picks_" + playerName + ".png");
        try {
            ImageIO.write(finalImage, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputFile;
    }

    public static SendPhoto createLeaderPickMessage(Long chatId, String playerName, List<Leader> leaders) {
        File imageFile = generateLeaderPickImage(playerName, leaders);

        return SendPhoto
                .builder()
                .chatId(chatId)
                .photo(new InputFile(imageFile))
                .caption(playerName + ": ")
                .build();
    }
}
