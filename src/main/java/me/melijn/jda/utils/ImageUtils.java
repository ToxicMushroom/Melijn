package me.melijn.jda.utils;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class ImageUtils {


    public static BufferedImage createPlane(int side, int color) {
        BufferedImage bufferedImage = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(new Color(color));
        graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        return bufferedImage;
    }

    private int getBrightness(int r, int g, int b) {
        return (int) Math.sqrt(r * r * .241 + g * g * .691 + b * b * .068);
    }

    public int[] getBurpleForPixel(int r, int g, int b) {
        int brightness = getBrightness(r, g, b);
        if (brightness >= 170) return new int[]{255, 255, 255}; //wit
        else if (brightness >= 85) return new int[]{114, 137, 218}; //blurple
        else return new int[]{78, 93, 148}; //dark blurple
    }

    public BufferedImage getBufferedImage(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        BufferedImage img = null;
        if (args.length > 0 && !args[0].isEmpty()) {
            User user = Helpers.getUserByArgsN(event, args[0]);
            if (user != null) {
                try {
                    img = ImageIO.read(new URL(user.getEffectiveAvatarUrl() + "?size=2048"));
                } catch (Exception e) {
                    event.reply("Something went wrong");
                }
            } else {
                try {
                    img = ImageIO.read(new URL(args[0]));
                } catch (Exception e) {
                    event.reply("That url isn't an image or is invalid");
                }
            }
        } else if (event.getMessage().getAttachments().size() > 0) {
            try {
                img = ImageIO.read(new URL(event.getMessage().getAttachments().get(0).getUrl() + "?size=2048"));
            } catch (Exception e) {
                event.reply("That attachment isn't an image");
            }
        } else {
            try {
                img = ImageIO.read(new URL(event.getAvatarUrl() + "?size=2048"));
            } catch (Exception e) {
                event.reply("Something went wrong");
            }
        }
        return img;
    }

    public int[] getSpookyForPixel(int r, int g, int b, int threshold) {
        int brightness = getBrightness(r, g, b);
        if (brightness >= threshold) return new int[]{255, 128, 0}; //ORANGE #FF8000
        else return new int[]{50, 50, 50}; //DARK #323232
    }
}
