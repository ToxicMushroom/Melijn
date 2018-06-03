package com.pixelatedsource.jda;

import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.commands.util.SetNotifications;
import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
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
    public static String guildOnly = "This command is to be used in guilds only";
    public static String noPerms = "You don't have the permission: ";
    public static final Logger LOG = LogManager.getLogger(PixelSniper.class.getName());
    public static Color EmbedColor = Color.decode("#00ffd8");
    public static int guildCount = 0;
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
            "loopqueue",
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
                    User toUnban = jda.retrieveUserById(bans.getLong("victimId")).complete();
                    try {
                        if (jda.getGuildById(bans.getLong("guildId")) != null)
                            PixelSniper.mySQL.unban(toUnban, jda.getGuildById(bans.getLong("guildId")), jda, true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                while (mutes.next()) {
                    User toUnmute = jda.retrieveUserById(mutes.getLong("victimId")).complete();
                    try {
                        if (jda.getGuildById(mutes.getLong("guildId")) != null)
                            PixelSniper.mySQL.unmute(jda.getGuildById(mutes.getLong("guildId")), toUnmute, jda, true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        Runnable runnable1 = () -> {
            if (dbl != null) dbl.setStats(jda.getSelfUser().getId(), guildCount == 0 ? jda.getGuilds().size() : guildCount);
            ArrayList<Long> votesList = PixelSniper.mySQL.getVoteList();
            for (long userId : SetNotifications.nextVotes.keySet()) {
                for (long targetId : SetNotifications.nextVotes.get(userId)) {
                    if (votesList.contains(targetId)) {
                        jda.retrieveUserById(userId).queue((u) ->
                                jda.retrieveUserById(targetId).queue((t) ->
                                        u.openPrivateChannel().queue((c) -> c.sendMessage("It's time to vote for **" + t.getName() + "#" + t.getDiscriminator() + "**").queue())));
                    }
                }
            }
        };
        executorService.scheduleAtFixedRate(runnable1, 5, 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(runnable, 1, 2, TimeUnit.SECONDS);
    }

    public static boolean hasPerm(Member member, String permission, int level) {
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (level == 0) {
            if (PixelSniper.mySQL.noOneHasPermission(member.getGuild().getIdLong(), permission)) return true;
        }
        return PixelSniper.mySQL.hasPermission(member.getGuild(), member.getUser().getIdLong(), permission) || PixelSniper.mySQL.hasPermission(member.getGuild(), member.getUser().getIdLong(), "*");
    }

    public static void waitForIt(User user) {
        Runnable run = () -> {
            MusicManager.usersRequest.remove(user);
            MusicManager.usersFormToReply.remove(user);
        };
        executorService.schedule(run, 30, TimeUnit.SECONDS);
    }

    public static String getDurationBreakdown(long millis) {
        if (millis < 0L) {
            return "error";
        }
        if (millis > 43200000000L) return "LIVE";
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
            if (hours < 10) sb.append(0);
            sb.append(hours);
            sb.append(":");
        }
        if (minutes != 0) {
            if (minutes < 10) sb.append(0);
            sb.append(minutes);
            sb.append(":");
        }
        if (seconds < 10) sb.append(0);
        sb.append(seconds);
        sb.append("s");

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

    public static User getUserByArgs(CommandEvent event, String arg) {//Without null
        User user = event.getAuthor();
        if (event.getMessage().getMentionedUsers().size() > 0) user = event.getMessage().getMentionedUsers().get(0);
        else if (arg.matches("\\d+") && event.getJDA().getUserById(arg) != null) user = event.getJDA().getUserById(arg);
        else if (event.getGuild() != null && event.getGuild().getMembersByName(arg, true).size() > 0) user = event.getGuild().getMembersByName(arg, true).get(0).getUser();
        else if (event.getGuild() != null && event.getGuild().getMembersByNickname(arg, true).size() > 0) user = event.getGuild().getMembersByNickname(arg, true).get(0).getUser();
        return user;
    }
    public static User getUserByArgsN(CommandEvent event, String arg) {//With null
        User user = null;
        if (event.getMessage().getMentionedUsers().size() > 0) user = event.getMessage().getMentionedUsers().get(0);
        else if (arg.matches("\\d+") && event.getJDA().getUserById(arg) != null) user = event.getJDA().getUserById(arg);
        else if (event.getGuild() != null && event.getGuild().getMembersByName(arg, true).size() > 0) user = event.getGuild().getMembersByName(arg, true).get(0).getUser();
        else if (event.getGuild() != null && event.getGuild().getMembersByNickname(arg, true).size() > 0) user = event.getGuild().getMembersByNickname(arg, true).get(0).getUser();
        return user;
    }

    public static boolean checkChannelPermission(Member member, TextChannel textChannel, Permission permission) {
        boolean toReturn = member.hasPermission(permission);
        if (member.getRoles().size() > 0) {
            if (textChannel.getPermissionOverride(member.getRoles().get(0)) != null) {
                if (textChannel.getPermissionOverride(member.getRoles().get(0)).getAllowed().contains(permission)) toReturn = true;
                if (textChannel.getPermissionOverride(member.getRoles().get(0)).getDenied().contains(permission)) toReturn = false;
            }
        }
        if (textChannel.getPermissionOverride(member) != null) {
            if (textChannel.getPermissionOverride(member).getAllowed().contains(permission)) toReturn = true;
            if (textChannel.getPermissionOverride(member).getDenied().contains(permission)) toReturn = false;
        }
        return toReturn;
    }

    public static long getChannelByArgsN(CommandEvent event, String arg) {
        long id = -1L;
        if (event.getMessage().getMentionedChannels().size() == 1) {
            id = event.getMessage().getMentionedChannels().get(0).getIdLong();
        } else if (arg.matches("\\d+") && event.getGuild().getTextChannelById(arg) != null) {
            id = Long.valueOf(arg);
        } else if (arg.equalsIgnoreCase("null")) {
            id = 0L;
        } else if (event.getGuild().getTextChannelsByName(arg, true).size() > 0) {
            id = event.getGuild().getTextChannelsByName(arg, true).get(0).getIdLong();
        }
        return id;
    }

    public static boolean checkVoiceChannelPermission(Member member, VoiceChannel voiceChannel, Permission permission) {
        boolean toReturn = member.hasPermission(permission);
        if (member.getRoles().size() > 0) {
            if (voiceChannel.getPermissionOverride(member.getRoles().get(0)) != null) {
                if (voiceChannel.getPermissionOverride(member.getRoles().get(0)).getDenied().contains(permission)) toReturn = false;
            }
        } else {
            if (voiceChannel.getPermissionOverride(member.getGuild().getRoleById(member.getGuild().getIdLong())) != null) {
                if (voiceChannel.getPermissionOverride(member.getGuild().getRoleById(member.getGuild().getIdLong())).getDenied().contains(permission)) toReturn = false;
            }
        }
        if (voiceChannel.getPermissionOverride(member) != null) {
            if (voiceChannel.getPermissionOverride(member).getDenied().contains(permission)) toReturn = false;
        }
        return toReturn;
    }
}
