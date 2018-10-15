package me.melijn.jda.blub;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CommandEvent {

    private final MessageReceivedEvent event;
    private String args;
    private final CommandClient client;
    private String executor;
    private int offset;

    public CommandEvent(MessageReceivedEvent event, String args, CommandClient client, String executor) {
        this.event = event;
        this.args = args == null ? "" : args;
        this.client = client;
        this.executor = executor;
        offset = event.getMessage().getContentRaw().split("\\s+")[0].equalsIgnoreCase("<@" + event.getJDA().getSelfUser().getId() + ">") ? 1 : 0;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    public Guild getGuild() {
        return event.getGuild();
    }

    public CommandClient getClient() {
        return client;
    }

    public User getAuthor() {
        return event.getAuthor();
    }

    public Member getMember() {
        return event.getMember();
    }

    public MessageChannel getChannel() {
        return event.getChannel();
    }

    public JDA getJDA() {
        return event.getJDA();
    }

    public Message getMessage() {
        return event.getMessage();
    }

    public void reply(String text) {
        if (text == null || text.equalsIgnoreCase("")) return;
        if (event.getPrivateChannel() != null) {
            event.getPrivateChannel().sendMessage(text).queue();
        } else {
            event.getTextChannel().sendMessage(text).queue();
        }
    }

    public void reply(MessageEmbed embed) {
        if (event.getGuild() == null) {
            event.getPrivateChannel().sendMessage(embed).queue();
        } else if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getTextChannel().sendMessage(embed).queue();
        else reply("I don't have permission to send embeds here.");
    }

    public void reply(String text, BufferedImage image) {
        try {
            if (event.getGuild() == null) {
                long time = System.currentTimeMillis();
                ImageIO.write(image, "png", new File(time + ".png"));
                if (new File(time + ".png").length() > 8_000_000) {
                    reply("The image is bigger then 8MB and cannot be send");
                    new File(time + ".png").delete();
                } else
                    event.getPrivateChannel().sendMessage(text).addFile(new File(time + ".png"), "finished.png").queue(
                            done -> new File(time + ".png").delete(),
                            failed -> new File(time + ".png").delete()
                    );
            } else if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ATTACH_FILES)) {
                long time = System.currentTimeMillis();
                ImageIO.write(image, "png", new File(time + ".png"));
                if (new File(time + ".png").length() > 8_000_000) {
                    reply("The image is bigger then 8MB and cannot be send");
                    new File(time + ".png").delete();
                } else event.getTextChannel().sendMessage(text).addFile(new File(time + ".png"), "finished.png").queue(
                        done -> new File(time + ".png").delete(),
                        failed -> new File(time + ".png").delete()
                );

            } else reply("I don't have permission to send images here.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reply(BufferedImage image) {
        try {
            if (event.getGuild() == null) {
                long time = System.currentTimeMillis();
                ImageIO.write(image, "png", new File(time + ".png"));
                if (new File(time + ".png").length() > 8_000_000) {
                    reply("The image is bigger then 8MB and cannot be send");
                    new File(time + ".png").delete();
                } else event.getPrivateChannel().sendFile(new File(time + ".png"), "finished.png").queue(
                        done -> new File(time + ".png").delete(),
                        failed -> new File(time + ".png").delete()
                );
            } else if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ATTACH_FILES)) {
                long time = System.currentTimeMillis();
                ImageIO.write(image, "png", new File(time + ".png"));
                if (new File(time + ".png").length() > 8_000_000) {
                    reply("The image is bigger then 8MB and cannot be send");
                    new File(time + ".png").delete();
                } else event.getTextChannel().sendFile(new File(time + ".png"), "finished.png").queue(
                        done -> new File(time + ".png").delete(),
                        failed -> new File(time + ".png").delete()
                );

            } else reply("I don't have permission to send images here.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFullAuthorName() {
        return getAuthor().getName() + "#" + getAuthor().getDiscriminator();
    }

    public long getAuthorId() {
        return getAuthor().getIdLong();
    }

    public TextChannel getTextChannel() {
        return event.getTextChannel();
    }

    public String getAvatarUrl() {
        return getAuthor().getEffectiveAvatarUrl();
    }

    public String getBotName() {
        return getJDA().getSelfUser().getName();
    }

    public String getExecutor() {
        return executor;
    }

    public int getOffset() {
        return offset;
    }
}
