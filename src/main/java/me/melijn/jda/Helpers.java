package me.melijn.jda;

import com.google.common.collect.Sets;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.NotificationType;
import me.melijn.jda.blub.RoleType;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.db.Variables;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

    public long lastRunTimer1 = -1, lastRunTimer2 = -1, lastRunTimer3 = -1, guildCount = 0;
    private final Melijn melijn;
    public boolean voteChecks = true;
    private final Pattern linuxUptimePattern = Pattern.compile("" +
            "(?:\\s+)?\\d+:\\d+:\\d+ up(?: (\\d+) days?,)?(?:\\s+(\\d+):(\\d+)|\\s+?(\\d+)\\s+?min).*"
    );
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public final Set<String> perms = Sets.newHashSet(
            "*",
            "pause",
            "splay.yt",
            "splay.sc",
            "splay.*",
            "play.yt",
            "play.sc",
            "play.url",
            "play.file",
            "play.*",
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
            "shoot",
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
            "bypass.cooldown",
            "restart"
    );

    public Helpers(Melijn melijn) {
        this.melijn = melijn;
    }

    public void startTimer(JDA jda, int i) {
        MySQL mySQL = melijn.getMySQL();
        Variables variables = melijn.getVariables();
        if (i == 0 || i == 1) {
            melijn.getTaskManager().scheduleRepeating(() -> {
                lastRunTimer1 = System.currentTimeMillis();
                mySQL.doUnbans(jda);
                mySQL.doUnmutes(jda);
                variables.timerAmount++;
            }, 2_000);
        }
        if (i == 0 || i == 2) {
            melijn.getTaskManager().scheduleRepeating(() -> {
                lastRunTimer2 = System.currentTimeMillis();

                Set<Long> votesList = melijn.getMySQL().getVoteList();
                Map<Long, Set<Long>> nextVoteMap = mySQL.getNotificationsMap(NotificationType.NEXTVOTE);
                for (long userId : nextVoteMap.keySet()) {
                    for (long targetId : nextVoteMap.getOrDefault(userId, Sets.newHashSet(-1L))) {
                        if (targetId == -1L || !votesList.contains(targetId)) continue;
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
                variables.timerAmount++;
            }, 60_000);
        }
        if (i == 0 || i == 3) {
            melijn.getTaskManager().scheduleRepeating(() -> {
                lastRunTimer3 = System.currentTimeMillis();
                melijn.getWebUtils().updateSpotifyCredentials();
                mySQL.updateVoteStreak();
                variables.timerAmount++;
            }, 1_800_000, 1_800_000);
        }
        if (i == 0 || i == 4) {
            melijn.getTaskManager().scheduleRepeating(() ->
                    new HashMap<>(melijn.getVariables().toLeaveTimeMap).forEach((guildId, time) -> {
                        if (melijn.getShardManager().getGuildCache().getElementById(guildId) == null) {
                            melijn.getVariables().toLeaveTimeMap.remove(guildId);
                            return;
                        }
                        if (time < System.currentTimeMillis()) { //Leaves after 5 minutes
                            MusicPlayer player = melijn.getLava().getAudioLoader().getPlayer(guildId);
                            melijn.getVariables().looped.remove(guildId);
                            melijn.getVariables().loopedQueues.remove(guildId);
                            melijn.getVariables().toLeaveTimeMap.remove(guildId);
                            player.getAudioPlayer().setPaused(false);
                            player.getTrackManager().clear();
                            player.stopTrack();
                        }
                    }), 60_000);
        }
        if (i == 0 || i == 5) {
            melijn.getTaskManager().scheduleRepeating(() -> {
                try {
                    postBotServerCounts();
                } catch (Exception e) {
                    melijn.getMessageHelper().printException(Thread.currentThread(), e, null, null);
                }
            }, 60_000, 150_000);
        }
    }

    private void postBotServerCounts() {
        long botId = melijn.getShardManager().getShards().get(0).getSelfUser().getIdLong();
        long serverCount = melijn.getShardManager().getGuildCache().size();
        long userCount = melijn.getShardManager().getUserCache().size();
        int shards = melijn.getShardManager().getShardsTotal();
        long voiceChannels = melijn.getShardManager().getShards().stream().mapToLong(
                (shard) -> shard.getVoiceChannels().stream().filter(
                        (vc) -> vc.getMembers().contains(vc.getGuild().getSelfMember())
                ).count()
        ).sum();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Variables variables = melijn.getVariables();
        JSONArray shardArray = new JSONArray();
        melijn.getShardManager().getShards().forEach(jda -> shardArray.put(jda.getGuildCache().size()));


        if (variables.dblAPI != null) {
            variables.dblAPI.setStats(Math.toIntExact(serverCount));
        }


        OkHttpClient okHttpClient = new OkHttpClient();

        if (variables.devineDBLToken != null) {
            Request request = new Request.Builder()
                    .url(String.format("https://divinediscordbots.com/bot/%d/stats", botId))
                    .post(new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("server_count", String.valueOf(serverCount))
                            .addFormDataPart("shards", String.valueOf(shards))
                            .build())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Authorization", variables.devineDBLToken)
                    .build();
            okHttpClient.newCall(request);
        } else logger.info("devineDBLToken is not set");

        if (variables.dblDotComToken != null) {
            Request request = new Request.Builder()
                    .url(String.format("https://discordbotlist.com/api/bots/%d/stats", botId))
                    .post(new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("guilds", String.valueOf(serverCount))
                            .addFormDataPart("users", String.valueOf(userCount))
                            .addFormDataPart("voice_connections", String.valueOf(voiceChannels))
                            .build())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Authorization", "Bot " + variables.dblDotComToken)
                    .build();
            okHttpClient.newCall(request);
        } else logger.info("dblDotComToken is not set");

        if (variables.blDotSpaceToken != null) {
            RequestBody requestBody = RequestBody.create(JSON, new JSONObject()
                    .put("shards", shardArray)
                    .toString(4));
            Request request = new Request.Builder()
                    .url(String.format("https://api.botlist.space/v1/bots/%d", botId))
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", variables.blDotSpaceToken)
                    .build();
            okHttpClient.newCall(request);
        } else logger.info("blDotSpaceToken is not set");

        if (variables.odDotXYZToken != null) {
            RequestBody requestBody = RequestBody.create(JSON, new JSONObject()
                    .put("guildCount", guildCount)
                    .toString(4));
            Request request = new Request.Builder()
                    .url(String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", botId))
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", variables.odDotXYZToken)
                    .build();
            okHttpClient.newCall(request);
        } else logger.info("odDotXYZToken is not set");
    }

    public void eval(CommandEvent event, ScriptEngine engine, String lang) {
        engine.put("event", event);
        engine.put("Melijn", Melijn.class);
        try {
            String output = engine.eval(event.getArgs()).toString();
            event.reply("" +
                    "Script input: ```" + lang + "\n" + event.getArgs() + "```\n" +
                    "Script evaluation output: ```" + output + "```"
            );
        } catch (ScriptException e) {
            event.reply(e.getMessage());
        }
    }

    public boolean hasPerm(Member member, String permission, int level) {
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (level == 0 && melijn.getMySQL().noOneHasPermission(member.getGuild().getIdLong(), permission)) return true;
        return melijn.getMySQL().hasPermission(member.getGuild(), member.getUser().getIdLong(), permission);
    }

    public String getFooterStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public String getFooterIcon() {
        return null;
    }

    public User getUserByArgs(CommandEvent event, String arg) {//Without null
        User user = getUserByArgsN(event, arg);
        if (user == null) user = event.getAuthor();
        return user;
    }

    public User getUserByArgsN(CommandEvent event, String arg) {//With null
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

    public long getTextChannelByArgsN(CommandEvent event, String arg) {
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

    public long getVoiceChannelByArgsN(CommandEvent event, String arg) {
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

    public void postMusicLog(long guildId, AudioTrack track) {
        if (melijn.getVariables().musicLogChannelCache.getUnchecked(guildId) == -1) return;
        Guild guild = melijn.getShardManager().getGuildById(guildId);
        TextChannel tc = guild.getTextChannelById(melijn.getVariables().musicLogChannelCache.getUnchecked(guildId));
        if (tc == null) {
            melijn.getMySQL().removeChannel(guildId, ChannelType.MUSIC_LOG);
            melijn.getVariables().musicLogChannelCache.invalidate(guildId);
            return;
        }
        if (!tc.canTalk()) return;
        tc.sendMessage(new Embedder(melijn.getVariables(), guild)
                .setTitle("Now playing")
                .setDescription("**[" + melijn.getMessageHelper().escapeMarkDown(track.getInfo().title) + "](" + track.getInfo().uri + ")** `" + melijn.getMessageHelper().getDurationBreakdown(track.getDuration()) + "`\n")
                .setThumbnail(melijn.getMessageHelper().getThumbnailURL(track.getInfo().uri))
                .setFooter(getFooterStamp(), null)
                .build()).queue();
    }

    public void verify(Guild guild, User user) {
        removeUnverified(guild, user);
        joinCode(guild, user);
    }

    public void removeUnverified(Guild guild, User user) {
        Map<Long, Long> newList = melijn.getVariables().unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
        newList.remove(user.getIdLong());
        melijn.getTaskManager().async(() -> {
            melijn.getMySQL().removeUnverifiedUser(guild.getIdLong(), user.getIdLong());
            melijn.getVariables().unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);
        });
        Member member = guild.getMember(user);
        Role unverifiedRole = guild.getRoleById(melijn.getVariables().unverifiedRoleCache.getUnchecked(guild.getIdLong()));
        if (member != null && guild.getRoleById(melijn.getVariables().unverifiedRoleCache.getUnchecked(guild.getIdLong())) != null) {
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) && guild.getSelfMember().canInteract(unverifiedRole) && guild.getSelfMember().canInteract(member)) {
                guild.getController().removeSingleRoleFromMember(member, guild.getRoleById(melijn.getVariables().unverifiedRoleCache.getUnchecked(guild.getIdLong()))).reason("verified user").queue();
            } else {
                user.openPrivateChannel().queue(channel ->
                        channel.sendMessage("" +
                                "You have been verified in " + guild.getName() + " discord server.\n" +
                                "**BUT** I do not have the permission to remove your unverified role.\n" +
                                "Please message a staff member about this issue."
                        ).queue());
            }
        }
    }

    public void joinCode(Guild guild, User user) {
        if (!melijn.getVariables().joinMessages.getUnchecked(guild.getIdLong()).isEmpty()) {
            TextChannel joinChannel = guild.getTextChannelById(melijn.getVariables().joinChannelCache.getUnchecked(guild.getIdLong()));
            if (joinChannel != null && guild.getSelfMember().hasPermission(joinChannel, Permission.MESSAGE_WRITE))
                joinChannel.sendMessage(melijn.getMessageHelper().variableFormat(melijn.getVariables().joinMessages.getUnchecked(guild.getIdLong()), guild, user)).queue();
        }
        if (guild.getSelfMember().getRoles().size() > 0) {
            Role joinRole = guild.getRoleById(melijn.getVariables().joinRoleCache.getUnchecked(guild.getIdLong()));
            if (joinRole != null && guild.getSelfMember().getRoles().get(0).canInteract(joinRole))
                guild.getController().addSingleRoleToMember(guild.getMember(user), joinRole).queue();
            melijn.getTaskManager().async(() -> {
                Role muteRole = guild.getRoleById(melijn.getMySQL().getRoleId(guild.getIdLong(), RoleType.MUTE));
                if (muteRole != null &&
                        melijn.getMySQL().isUserMuted(user.getIdLong(), guild.getIdLong()) &&
                        guild.getSelfMember().getRoles().get(0).canInteract(muteRole))
                    guild.getController().addSingleRoleToMember(guild.getMember(user), muteRole).queue();
            });
        }
        List<Long> forcedRoles = melijn.getMySQL().getForcedRoles(guild.getIdLong(), user.getIdLong());
        if (guild.getSelfMember().canInteract(guild.getMember(user))) {
            forcedRoles.forEach(roleId -> {
                Role role = guild.getRoleById(roleId);
                if (guild.getSelfMember().canInteract(role)) {
                    guild.getController().addSingleRoleToMember(guild.getMember(user), role).queue();
                }
            });
        }
    }

    public Role getRoleByArgs(CommandEvent event, String arg) {
        if (!arg.matches("\\s+") && !arg.isEmpty()) {
            if (event.getMessage().getMentionedRoles().size() > 0) return event.getMessage().getMentionedRoles().get(0);
            else if (arg.matches("\\d+") && event.getGuild().getRoleById(arg) != null)
                return event.getGuild().getRoleById(arg);
            else if (event.getGuild() != null && event.getGuild().getRolesByName(arg, true).size() > 0)
                return event.getGuild().getRolesByName(arg, true).get(0);
        }
        return null;
    }

    public void retrieveUserByArgs(CommandEvent event, String arg, Consumer<User> target) {
        User user = getUserByArgsN(event, arg);
        if (user != null)
            target.accept(user);
        else if (arg.matches("\\d+"))
            event.getJDA().asBot().getShardManager().retrieveUserById(arg).queue(target, failure -> target.accept(event.getAuthor()));
        else target.accept(event.getAuthor());
    }

    public void retrieveUserByArgsN(CommandEvent event, String arg, Consumer<User> target) {
        User user = getUserByArgsN(event, arg);
        if (user != null)
            target.accept(user);
        else if (arg.matches("\\d+"))
            event.getJDA().asBot().getShardManager().retrieveUserById(arg).queue(target, failure -> target.accept(null));
        else target.accept(null);
    }

    public long parseTimeFromArgs(String[] args) {
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
            case 3:
            default: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                    millis = 3600000 * Short.parseShort(args[0]) + 60000 * Short.parseShort(args[1]) + 1000 * Short.parseShort(args[2]);
                }
                break;
            }
        }
        return millis;
    }

    public boolean canNotInteract(CommandEvent event, User target) {
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

    public boolean canNotInteract(CommandEvent event, Role target) {
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

    public boolean isJSONObjectValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ignored) {
            return false;
        }
        return true;
    }

    public long getSystemUptime() {
        try {
            long uptime = -1;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Process uptimeProc = Runtime.getRuntime().exec("net stats workstation");
                BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("Statistieken vanaf")) {
                        SimpleDateFormat format = new SimpleDateFormat("'Statistieken vanaf' dd/MM/yyyy hh:mm:ss"); //Dutch windows version
                        Date bootTime = format.parse(line);
                        uptime = System.currentTimeMillis() - bootTime.getTime();
                        break;
                    } else if (line.startsWith("Statistics since")) {
                        SimpleDateFormat format = new SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss a"); //English windows version
                        Date bootTime = format.parse(line);
                        uptime = System.currentTimeMillis() - bootTime.getTime();
                        break;
                    }
                }
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                Process uptimeProc = Runtime.getRuntime().exec("uptime"); //Parse time to groups if possible
                BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
                String line = in.readLine();
                if (line == null) return uptime;
                Matcher matcher = linuxUptimePattern.matcher(line);

                if (!matcher.find()) return uptime; //Extract ints out of groups
                String _days = matcher.group(1);
                String _hours = matcher.group(2);
                String _minutes = matcher.group(3) == null ? matcher.group(4) : matcher.group(3);
                int days = _days != null ? Integer.parseInt(_days) : 0;
                int hours = _hours != null ? Integer.parseInt(_hours) : 0;
                int minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
                uptime = (minutes * 60_000) + (hours * 60_000 * 60) + (days * 60_000 * 60 * 24);
            }
            return uptime;
        } catch (Exception e) {
            return -1;
        }
    }
}
