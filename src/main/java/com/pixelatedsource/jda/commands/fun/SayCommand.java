package com.pixelatedsource.jda.commands.fun;

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
                    image = ImageIO.read(new File(resourcename));
                    Graphics g = image.getGraphics();
                    g.setFont(g.getFont().deriveFont(40f));
                    if (event.getArgs().length() < 26) {
                        g.drawString(event.getArgs(), 650, 200);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        String[] parts = event.getArgs().split("\\s+");
                        for (String part : parts) {
                            if (part.length() > 25) {
                                String[] characters = part.split("");
                                int i = 0;
                                for (String charl : characters) {
                                    sb.append(charl);
                                    if (i++ == 22) {
                                        sb.append("-\n");
                                        i = 0;
                                    }
                                }
                            } else if ((sb.toString().split("\n")[sb.toString().split("\n").length - 1].length() + part.length()) > 28) {
                                sb.append("\n").append(part);
                            } else {
                                if (sb.toString().length() > 0) sb.append(" ");
                                sb.append(part);
                            }
                        }
                        int i = 0;
                        String[] lines = sb.toString().split("\n");
                        for (String line : lines) {
                            g.drawString(line, 640, 220 - (40 * lines.length/2) + (i++ * 40));

                        }
                    }
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
