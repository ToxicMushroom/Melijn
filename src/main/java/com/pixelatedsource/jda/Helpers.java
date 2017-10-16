package com.pixelatedsource.jda;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Helpers {

    public static long starttime;

    public static final Logger LOG = LogManager.getLogger(PixelatedBot.class.getName());
    public static Color EmbedColor = Color.decode("#00ffd8");
    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days != 0) {
            sb.append(days);
            sb.append("d ");
        }
        if (hours != 0) {
            sb.append(hours);
            sb.append("h ");
        }
        if (minutes != 0) {
            sb.append(minutes);
            sb.append("min ");
        }
        sb.append(seconds);
        sb.append("s ");

        return (sb.toString());
    }
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static void ScheduleClose(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect())
            return;

        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

    public static String getOnlineTime() {
        return getDurationBreakdown(System.currentTimeMillis() - starttime);
    }

    public static String getFooterStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return "ToxicMushroom | " + simpleDateFormat.format(date);
    }

    public static String getFooterIcon() {
        return "https://i.imgur.com/1wj6Jlr.png";
    }

    public static void DefaultEmbed(String title, String Content, TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(EmbedColor);
        eb.setDescription(Content);
        eb.setTitle(title);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        channel.sendMessage(eb.build()).queue();
    }

}
