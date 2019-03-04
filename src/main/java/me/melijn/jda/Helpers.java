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
import org.json.JSONException;
import org.json.JSONObject;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class Helpers {

    public long lastRunTimer1 = -1, lastRunTimer2 = -1, lastRunTimer3 = -1, guildCount = 0;
    private final Melijn melijn;
    public boolean voteChecks = true;
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
        melijn.getVariables().serverBlackList.addAll(Arrays.asList(110373943822540800L, 264445053596991498L));
        melijn.getVariables().userBlackList.addAll(Arrays.asList(244397405846372354L, 324570870800449548L, 444348640450969600L));
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
                if (variables.dblAPI != null)
                    variables.dblAPI.setStats(Math.toIntExact(guildCount == 0 ? jda.asBot().getShardManager().getGuildCache().size() : guildCount));
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
            melijn.getTaskManager().scheduleRepeating(() -> melijn.getVariables().toLeaveTimeMap.forEach((guildId, time) -> {
                if (time < System.currentTimeMillis()) { //Leaves after 5 minutes
                    MusicPlayer player = melijn.getLava().getAudioLoader().getPlayer(guildId);
                    melijn.getVariables().looped.remove(guildId);
                    melijn.getVariables().loopedQueues.remove(guildId);
                    player.getAudioPlayer().setPaused(false);
                    player.getTrackManager().clear();
                    player.stopTrack();
                }
            }), 60_000);
        }
    }

    public void eval(CommandEvent event, ScriptEngine engine) {
        ScriptContext context = engine.getContext();
        context.setAttribute("event", event, ScriptContext.ENGINE_SCOPE);
        context.setAttribute("Melijn", Melijn.class, ScriptContext.ENGINE_SCOPE);
        StringWriter writer = new StringWriter();
        context.setWriter(writer);

        try {
            engine.eval(event.getArgs());
        } catch (ScriptException e) {
            event.reply(e.getMessage());
        }

        String output = writer.toString();
        event.reply("Script output: " + output);
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
}
