package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SayCommand extends Command {

    public SayCommand() {
        this.commandName = "say";
        this.description = "Makes the bot say stuff";
        this.usage = PREFIX + commandName + " <message>";
        this.aliases = new String[]{"zeg"};
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getArgs().length() > 0) {
                final BufferedImage image;
                try {
                    String resourcename = event.getExecutor().equalsIgnoreCase("zeg") ? "melijn_zegt.png" : "melijn_says.png";
                    if (getClass().getClassLoader().getResource(resourcename) == null) return;
                    image = ImageIO.read(new File(getClass().getClassLoader().getResource(resourcename).getFile()));
                    Graphics g = image.getGraphics();
                    g.setFont(g.getFont().deriveFont(40f));
                    g.drawString(event.getArgs(), 650, 100);
                    g.dispose();
                    String imageName = String.valueOf(System.currentTimeMillis());
                    ImageIO.write(image, "png", new File(imageName + ".png"));
                    event.getTextChannel().sendFile(new File(imageName + ".png")).queue(q -> new File(imageName + ".png").delete());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
