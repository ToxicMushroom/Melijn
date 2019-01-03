package me.melijn.jda;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.NotificationType;
import me.melijn.jda.commands.management.SetLogChannelCommand;
import me.melijn.jda.events.JoinLeave;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Helpers {

    public static long lastRunTimer1 = -1, lastRunTimer2 = -1, lastRunTimer3 = -1;
    public static long startTime = 0;
    public static final String guildOnly = "This command is to be used in guilds only";
    public static final String nsfwOnly = "This command is to be used in (not safe for work) better known as [NSFW] channels only and can contain 18+ content";
    public static final String noPerms = "You don't have the permission: ";
    public static final int embedColor = -16711720;
    public static boolean voteChecks = true;
    public static long guildCount = 0;
    public static final List<String> perms = Arrays.asList(
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
            "role",
            "roles",
            "dog",
            "about",
            "ping",
            "setprefix",
            "setlogchannel",
            "tempban",
            "unban",
            "setmusicchannel",
            "setstreamermode",
            "setstreamurl",
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
            "setjoinrole",
            "shuffle",
            "setmusiclogchannel",
            "setnotifications",
            "lewd",
            "punch",
            "wasted",
            "highfive",
            "dab",
            "shrug",
            "cry",
            "kick",
            "bypass.sameVoiceChannel",
            "summon",
            "nyancat",
            "clearchannel",
            "shards",
            "setVerificationCode",
            "setVerificationChannel",
            "setUnverifiedRole",
            "setVerificationThreshold",
            "verify",
            "invert",
            "blurple",
            "urban",
            "bird",
            "enable",
            "disabled",
            "metrics",
            "settings",
            "slowmode",
            "unicode",
            "alpaca",
            "stats",
            "kiss",
            "hug",
            "spookify",
            "SetSelfRoleChannel",
            "SelfRole",
            "CustomCommand",
            "poll",
            "dice",
            "SetEmbedColor",
            "emotes",
            "cooldown",
            "SetVerificationType",
            "bypass.cooldown"
    );

    public static void startTimer(JDA jda, int i) {
        if (i == 0 || i == 1) {
            TaskScheduler.scheduleRepeating(() -> {
                lastRunTimer1 = System.currentTimeMillis();
                Melijn.mySQL.doUnbans(jda);
                Melijn.mySQL.doUnmutes(jda);
            }, 2_000);
        }
        if (i == 0 || i == 2) {
            TaskScheduler.scheduleRepeating(() -> {
                lastRunTimer2 = System.currentTimeMillis();
                if (JoinLeave.dblAPI != null)
                    JoinLeave.dblAPI.setStats(Math.toIntExact(guildCount == 0 ? jda.asBot().getShardManager().getGuildCache().size() : guildCount));
                TLongList votesList = Melijn.mySQL.getVoteList();
                TLongObjectMap<TLongList> nextVoteMap = Melijn.mySQL.getNotificationsMap(NotificationType.NEXTVOTE);
                for (long userId : nextVoteMap.keys()) {
                    for (long targetId : nextVoteMap.get(userId).toArray()) {
                        if (votesList.contains(targetId)) {
                            jda.asBot().getShardManager().retrieveUserById(userId).queue((u) -> {
                                if (userId != targetId)
                                    jda.asBot().getShardManager().retrieveUserById(targetId).queue((t) ->
                                            u.openPrivateChannel().queue((c) -> c.sendMessage(String.format("It's time to vote for **%#s**", t)).queue()));
                                else {
                                    u.openPrivateChannel().queue((c) -> c.sendMessage(String.format("It's time to vote for **%#s**", u)).queue());
                                }
                            });
                        }
                    }
                }
            }, 60_000);
        }
        if (i == 0 || i == 3) {
            TaskScheduler.scheduleRepeating(() -> {
                lastRunTimer3 = System.currentTimeMillis();
                WebUtils.getWebUtilsInstance().updateSpotifyCredentials();
                Melijn.mySQL.updateVoteStreak();
            }, 1_800_000, 1_800_000);
        }
    }

    public static boolean hasPerm(Member member, String permission, int level) {
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (level == 0 && Melijn.mySQL.noOneHasPermission(member.getGuild().getIdLong(), permission)) return true;
        return Melijn.mySQL.hasPermission(member.getGuild(), member.getUser().getIdLong(), permission);
    }

    public static String getDurationBreakdown(long milliseconds) {
        long millis = milliseconds;
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
        appendTimePart(hours, sb);
        appendTimePart(minutes, sb);
        if (seconds < 10) sb.append(0);
        sb.append(seconds);
        sb.append("s");

        return (sb.toString());
    }

    private static void appendTimePart(long hours, StringBuilder sb) {
        if (hours != 0) {
            if (hours < 10) sb.append(0);
            sb.append(hours);
            sb.append(":");
        }
    }

    public static String getFooterStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public static String getFooterIcon() {
        return null;
    }

    public static User getUserByArgs(CommandEvent event, String arg) {//Without null
        User user = getUserByArgsN(event, arg);
        if (user == null) user = event.getAuthor();
        return user;
    }

    public static User getUserByArgsN(CommandEvent event, String arg) {//With null
        User user = null;
        if (!arg.matches("\\s+") && !arg.isEmpty()) {
            if (event.getMessage().getMentionedUsers().size() > event.getOffset())
                user = event.getMessage().getMentionedUsers().get(event.getOffset());
            else if (arg.matches("\\d+") && event.getJDA().getUserById(arg) != null)
                user = event.getJDA().asBot().getShardManager().getUserById(arg);
            else if (event.getGuild() != null && event.getGuild().getMembersByName(arg, true).size() > 0)
                user = event.getGuild().getMembersByName(arg, true).get(0).getUser();
            else if (event.getGuild() != null && event.getGuild().getMembersByNickname(arg, true).size() > 0)
                user = event.getGuild().getMembersByNickname(arg, true).get(0).getUser();
        }
        return user;
    }

    public static long getTextChannelByArgsN(CommandEvent event, String arg) {
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

    public static long getVoiceChannelByArgsN(CommandEvent event, String arg) {
        long id = -1L;
        if (event.getMessage().getMentionedChannels().size() == 1) {
            id = event.getMessage().getMentionedChannels().get(0).getIdLong();
        } else if (arg.matches("\\d+") && event.getGuild().getVoiceChannelById(arg) != null) {
            id = Long.valueOf(arg);
        } else if (arg.equalsIgnoreCase("null")) {
            id = 0L;
        } else if (event.getGuild().getVoiceChannelsByName(arg, true).size() > 0) {
            id = event.getGuild().getVoiceChannelsByName(arg, true).get(0).getIdLong();
        }
        return id;
    }

    public static void postMusicLog(long guildId, AudioTrack track) {
        if (SetLogChannelCommand.musicLogChannelCache.getUnchecked(guildId) == -1) return;
        Guild guild = Melijn.getShardManager().getGuildById(guildId);
        TextChannel tc = guild.getTextChannelById(SetLogChannelCommand.musicLogChannelCache.getUnchecked(guildId));
        if (tc == null) {
            Melijn.mySQL.removeChannel(guildId, ChannelType.MUSIC_LOG);
            SetLogChannelCommand.musicLogChannelCache.invalidate(guildId);
            return;
        }
        if (!tc.canTalk()) return;
        tc.sendMessage(new Embedder(guild)
                .setTitle("Now playing")
                .setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** `" + Helpers.getDurationBreakdown(track.getDuration()) + "`\n")
                .setThumbnail(MessageHelper.getThumbnailURL(track.getInfo().uri))
                .setFooter(Helpers.getFooterStamp(), null)
                .build()).queue();
    }

    public static Role getRoleByArgs(CommandEvent event, String arg) {
        if (!arg.matches("\\s+") && !arg.isEmpty()) {
            if (event.getMessage().getMentionedRoles().size() > 0) return event.getMessage().getMentionedRoles().get(0);
            else if (arg.matches("\\d+") && event.getGuild().getRoleById(arg) != null)
                return event.getGuild().getRoleById(arg);
            else if (event.getGuild() != null && event.getGuild().getRolesByName(arg, true).size() > 0)
                return event.getGuild().getRolesByName(arg, true).get(0);
        }
        return null;
    }

    public static void retrieveUserByArgs(CommandEvent event, String arg, Consumer<User> success) {
        User user = getUserByArgsN(event, arg);
        if (user != null)
            success.accept(user);
        else if (arg.matches("\\d+"))
            event.getJDA().asBot().getShardManager().retrieveUserById(arg).queue(success);
        else success.accept(event.getAuthor());
    }

    public static void retrieveUserByArgsN(CommandEvent event, String arg, Consumer<User> success) {
        User user = getUserByArgsN(event, arg);
        if (user != null)
            success.accept(user);
        else if (arg.matches("\\d+"))
            event.getJDA().asBot().getShardManager().retrieveUserById(arg).queue(success);
        else success.accept(null);
    }

    public static long parseTimeFromArgs(String[] args) {
        long millis = -1;
        switch (args.length) {
            case 0:
                break;
            case 1: {
                if (args[0].matches("(\\d)|(\\d\\d)")) {
                    millis = 1000 * Short.parseShort(args[0]);
                }
                break;
            }
            case 2: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)")) {
                    millis = 60000 * Short.parseShort(args[0]) + 1000 * Short.parseShort(args[1]);
                }
                break;
            }
            case 3: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                    millis = 3600000 * Short.parseShort(args[0]) + 60000 * Short.parseShort(args[1]) + 1000 * Short.parseShort(args[2]);
                }
                break;
            }
            default: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                    millis = 3600000 * Short.parseShort(args[0]) + 60000 * Short.parseShort(args[1]) + 1000 * Short.parseShort(args[2]);
                }
                break;
            }
        }
        return millis;
    }

    public static boolean canNotInteract(CommandEvent event, User target) {
        if (event.getGuild().getMember(target).getRoles().size() > 0 && event.getGuild().getSelfMember().getRoles().size() > 0) {
            if (!event.getGuild().getSelfMember().getRoles().get(0).canInteract(event.getGuild().getMember(target).getRoles().get(0))) {
                event.reply("Can't modify a member with higher or equal highest role than myself");
                return true;
            }
        } else if (event.getGuild().getSelfMember().getRoles().size() == 0) {
            event.reply("Can't modify a member with higher or equal highest role than myself");
            return true;
        }
        return false;
    }

    public static boolean canNotInteract(CommandEvent event, Role target) {
        if (event.getGuild().getSelfMember().getRoles().size() > 0) {
            if (!event.getGuild().getSelfMember().getRoles().get(0).canInteract(target)) {
                event.reply("Can't modify a member with higher or equal highest role than myself");
                return true;
            }
        } else {
            event.reply("Can't modify a member with higher or equal highest role than myself");
            return true;
        }
        return false;
    }

    public static boolean isJSONObjectValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ignored) {
            return false;
        }
        return true;
    }
}
