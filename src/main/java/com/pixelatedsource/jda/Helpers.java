package com.pixelatedsource.jda;

import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.discordbots.api.client.DiscordBotListAPI;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Helpers {

    public static long lastRunMillis;
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static long starttime;
    public static String guildOnly = "This is a guildonly command";
    public static String noPerms = "You don't have the permission: ";
    public static final Logger LOG = LogManager.getLogger(PixelSniper.class.getName());
    public static Color EmbedColor = Color.decode("#00ffd8");
    public static ArrayList<String> perms = new ArrayList<>(Arrays.asList(
            "pause",
            "splay.yt",
            "splay.sc",
            "splay.link",
            "splay.*",
            "play.yt",
            "play.sc",
            "play.link",
            "skip",
            "skipx",
            "stop",
            "volume",
            "loop",
            "queue",
            "clear",
            "loop",
            "resume",
            "userinfo",
            "np",
            "remove",
            "cat",
            "t2e",
            "perm.add",
            "perm.remove",
            "perm.clear",
            "perm.view",
            "perm.copy",
            "perm.*",
            "*",
            "play.*",
            "guildinfo",
            "roleinfo",
            "dog",
            "alpaca",
            "about",
            "ping",
            "setprefix",
            "setlogchannel",
            "tempban",
            "unban",
            "setmusicchannel",
            "setstreamermode",
            "setstreamurl",
            "emote.claim",
            "emote.delete",
            "warn",
            "purge",
            "ban",
            "history",
            "mute",
            "setmuterole",
            "tempmute",
            "unmute",
            "avatar",
            "potato",
            "filter",
            "pat",
            "slap",
            "triggered",
            "setjoinleavechannel",
            "setjoinmessage",
            "setleavemessage",
            "setjoinrole"
    ));

    public static void startTimer(JDA jda, DiscordBotListAPI dbl) {
        Runnable runnable = () -> {

            lastRunMillis = System.currentTimeMillis();
            try {
                ResultSet bans = PixelSniper.mySQL.query("SELECT * FROM active_bans WHERE endTime < " + System.currentTimeMillis());
                ResultSet mutes = PixelSniper.mySQL.query("SELECT * FROM active_mutes WHERE endTime < " + System.currentTimeMillis());

                while (bans.next()) {
                    User toUnban = jda.retrieveUserById(bans.getString("victimId")).complete();
                    PixelSniper.mySQL.unban(toUnban, jda.getGuildById(bans.getString("guildId")), jda, true);
                }

                while (mutes.next()) {
                    User toUnmute = jda.retrieveUserById(mutes.getString("victimId")).complete();
                    PixelSniper.mySQL.unmute(toUnmute, jda.getGuildById(mutes.getString("guildId")), jda, true);
                }

                bans.close();
                mutes.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        Runnable runnable1 = () -> {
            if (dbl != null) dbl.setStats(jda.getSelfUser().getId(), jda.getGuilds().size());
        };
        executorService.scheduleAtFixedRate(runnable1, 5, 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(runnable, 1, 2, TimeUnit.SECONDS);
    }

    public static boolean hasPerm(Member member, String permission, int level) {
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (level == 0) {
            if (PixelSniper.mySQL.noOneHasPermission(member.getGuild(), permission)) return true;
        }
        return PixelSniper.mySQL.hasPermission(member.getGuild(), member.getUser(), permission) || PixelSniper.mySQL.hasPermission(member.getGuild(), member.getUser(), "*");
    }

    public static void waitForIt(User user) {
        Runnable run = () -> {
            MusicManager.usersRequest.remove(user);
            MusicManager.usersFormToReply.remove(user);
        };
        executorService.schedule(run, 30, TimeUnit.SECONDS);
    }

    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            return "error";
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
            sb.append("m ");
        }
        sb.append(seconds);
        sb.append("s ");

        return (sb.toString());
    }

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void ScheduleClose(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect()) return;
        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

    public static String getOnlineTime() {
        return getDurationBreakdown(System.currentTimeMillis() - starttime);
    }

    public static String getFooterStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public static String getFooterIcon() {
        return null;
    }

    public static String numberToString(int i) {
        switch (i) {
            case 0:
                return "zero";
            case 1:
                return "one";
            case 2:
                return "two";
            case 3:
                return "three";
            case 4:
                return "four";
            case 5:
                return "five";
            case 6:
                return "six";
            case 7:
                return "seven";
            case 8:
                return "eight";
            case 9:
                return "nine";
            default:
                return "zero";
        }
    }

}
