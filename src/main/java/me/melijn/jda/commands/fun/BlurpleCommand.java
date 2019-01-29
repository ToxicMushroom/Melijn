package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static me.melijn.jda.Melijn.PREFIX;

public class BlurpleCommand extends Command {



    public BlurpleCommand() {
        this.commandName = "blurple";
        this.usage = PREFIX + commandName + " [image]";
        this.description = "Blurpifies an image";
        this.category = Category.FUN;
        this.id = 3;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            ImageUtils imageUtils = new ImageUtils();
            BufferedImage img = imageUtils.getBufferedImage(event);
            if (img == null) return;

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int pixel = img.getRGB(x, y);

                    int a = (pixel >> 24) & 0xff;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;

                    int[] newcolor = imageUtils.getBurpleForPixel(r, g, b);
                    r = newcolor[0];
                    g = newcolor[1];
                    b = newcolor[2];
                    pixel = (a << 24) | (r << 16) | (g << 8) | b;

                    img.setRGB(x, y, pixel);
                }
            }
            event.reply(img);
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
