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

    public static File generateLeaderPickImage(String playerName, List<Leader> leaders) {
        int iconSize = 80;
        int padding = 15;   // space between icons
        int maxTextWidth = iconSize - 5;
        int maxTextRows = 5;
        int textLineHeight = 18;
        int textPadding = 12; // space between icon and name of the leader

        int width = leaders.size() * (iconSize + padding);
        int height = iconSize + (maxTextRows * textLineHeight) + textPadding + 10;

        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = finalImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setFont(new Font("Verdana", Font.BOLD, 14));
        g.setColor(Color.BLACK);

        int x = padding / 2;
        int y = padding;

        for (Leader leader : leaders) {
            BufferedImage leaderIcon = null;
            try {
                leaderIcon = ImageIO.read(new File(leader.getPicPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Image scaledIcon = leaderIcon.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            g.drawImage(scaledIcon, x, y, null);

            int textStartY = y + iconSize + textPadding;

            drawWrappedText(g, leader.getFullName(), x, textStartY, maxTextWidth, maxTextRows);

            x += iconSize + padding;
        }

        g.dispose();

        // temporary file which will be deleted when lobby is closed
        File outputFile = null;
        try {
            outputFile = File.createTempFile("leader_picks_", ".png");
            ImageIO.write(finalImage, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputFile;
    }

    private static void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int maxRows) {
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int rowCount = 0;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if ((word.equalsIgnoreCase("of") || word.equalsIgnoreCase("de") || word.equalsIgnoreCase("the"))
                    && i + 1 < words.length) {
                word += " " + words[i + 1];
                i++;
            }

            String testLine = currentLine.toString() + " " + word;
            int testWidth = metrics.stringWidth(testLine.trim());

            if (testWidth > maxWidth && !currentLine.toString().isEmpty()) {
                drawCenteredString(g, currentLine.toString().trim(), x, y, maxWidth);
                y += lineHeight;
                rowCount++;

                currentLine = new StringBuilder(word);

                if (rowCount >= maxRows) {
                    break;
                }
            } else {
                currentLine.append(" ").append(word);
            }
        }

        if (!currentLine.toString().trim().isEmpty() && rowCount < maxRows) {
            drawCenteredString(g, currentLine.toString().trim(), x, y, maxWidth);
        }
    }

    private static void drawCenteredString(Graphics2D g, String text, int x, int y, int maxWidth) {
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int startX = x + (maxWidth - textWidth) / 2;
        g.drawString(text, startX, y);
    }

    public static SendPhoto createLeaderPickMessage(Long chatId, String playerName, List<Leader> leaders) {
        File imageFile = generateLeaderPickImage(playerName, leaders);

        SendPhoto sp = SendPhoto
                .builder()
                .chatId(chatId)
                .photo(new InputFile(imageFile))
                .caption(playerName + "`s pool")
                .build();

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                imageFile.delete();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        return sp;
    }
}