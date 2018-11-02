package me.melijn.jda.events;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gnu.trove.list.TLongList;
import me.melijn.jda.Config;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.RoleType;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.commands.music.SPlayCommand;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JoinLeave extends ListenerAdapter {

    public static final LoadingCache<Long, TLongList> unVerifiedGuildMembersCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public TLongList load(@NotNull Long key) {
                    return Melijn.mySQL.getUnverifiedMembers(key);
                }
            });
    public static DiscordBotListAPI dblAPI = null;
    private boolean started = false;

    @Override
    public void onReady(ReadyEvent event) {
        Helpers.startTime = System.currentTimeMillis();
        ShardManager shardManager = event.getJDA().asBot().getShardManager();
        if (started || shardManager.getShardCache().stream().filter(shard -> shard.getStatus().equals(JDA.Status.CONNECTED)).count() == shardManager.getShardsTotal())
            return;
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> MessageHelper.printException(thread, exception, null, null));
        dblAPI = new DiscordBotListAPI.Builder()
                .token(Config.getConfigInstance().getValue("dbltoken"))
                .botId(event.getJDA().getSelfUser().getId())
                .build();
        Helpers.startTimer(event.getJDA(), 0);
        MusicManager musicManager = MusicManager.getManagerInstance();
        for (JSONObject queue : Melijn.mySQL.getQueues()) {
            Guild guild = shardManager.getGuildById(queue.getLong("guildId"));
            if (guild != null) {
                VoiceChannel vc = guild.getVoiceChannelById(queue.getLong("channelId"));
                if (vc != null) {
                    SPlayCommand.isNotConnectedOrConnecting(vc);
                    boolean pause = queue.getBoolean("paused");
                    String[] urls = queue.getString("urls").split("\n");
                    musicManager.getPlayer(guild).getAudioPlayer().setPaused(pause);
                    for (String url : urls) {
                        if (!url.startsWith("#0 "))
                            musicManager.loadSimpleTrack(guild, url.replaceFirst("#\\d+ ", ""));
                    }
                }
            }
        }
        Melijn.mySQL.clearQueues();

        started = true;
    }

    @Override
    public void onShutdown(ShutdownEvent event) {

    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong())) {
            return;
        }
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (joinedUser.isBot() && joinedUser.equals(guild.getSelfMember().getUser()) && EvalCommand.serverBlackList.contains(guild.getOwnerIdLong()))
            guild.leave().queue();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        if (SetVerificationChannel.verificationChannelsCache.getUnchecked(guild.getIdLong()) != -1) {
            TextChannel verificationChannel = guild.getTextChannelById(SetVerificationChannel.verificationChannelsCache.getUnchecked(guild.getIdLong()));
            if (verificationChannel != null) {
                TLongList newList = unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
                newList.add(joinedUser.getIdLong());
                Melijn.mySQL.addUnverifiedUser(guild.getIdLong(), joinedUser.getIdLong());
                unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);
                if (guild.getRoleById(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(guild.getIdLong())) != null)
                    guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(guild.getIdLong()))).reason("unverified user").queue();
            } else {
                TaskScheduler.async(() -> {
                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION);
                    SetVerificationChannel.verificationChannelsCache.invalidate(guild.getIdLong());
                });
            }
        } else {
            try {
                joinCode(guild, joinedUser);
            } catch (ExecutionException ignore) {
            }
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        if (unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong()).contains(leftUser.getIdLong())) {
            removeUnverified(guild, leftUser);
        } else {
            TaskScheduler.async(() -> {
                String message = SetLeaveMessageCommand.leaveMessages.getUnchecked(guild.getIdLong());
                if (!message.isBlank()) {
                    TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()));
                    if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                        welcomeChannel.sendMessage(variableFormat(message, guild, leftUser)).queue();
                }
            });
        }
    }

    public static void verify(Guild guild, User user) {
        removeUnverified(guild, user);
        try {
            joinCode(guild, user);
        } catch (ExecutionException ignore) {
        }
    }

    private static void joinCode(Guild guild, User user) throws ExecutionException {
        if (!SetJoinMessageCommand.joinMessages.getUnchecked(guild.getIdLong()).isBlank()) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()));
            if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                welcomeChannel.sendMessage(variableFormat(SetJoinMessageCommand.joinMessages.getUnchecked(guild.getIdLong()), guild, user)).queue();
        }
        if (guild.getSelfMember().getRoles().size() > 0) {
            Role joinRole = guild.getRoleById(SetJoinRoleCommand.joinRoleCache.get(guild.getIdLong()));
            if (joinRole != null && guild.getSelfMember().getRoles().get(0).canInteract(joinRole))
                guild.getController().addSingleRoleToMember(guild.getMember(user), joinRole).queue();
            TaskScheduler.async(() -> {
                Role muteRole = guild.getRoleById(Melijn.mySQL.getRoleId(guild.getIdLong(), RoleType.MUTE));
                if (muteRole != null &&
                        Melijn.mySQL.isUserMuted(user.getIdLong(), guild.getIdLong()) &&
                        guild.getSelfMember().getRoles().get(0).canInteract(muteRole))
                    guild.getController().addSingleRoleToMember(guild.getMember(user), muteRole).queue();
            });
        }
    }

    private static void removeUnverified(Guild guild, User user) {
        TLongList newList = unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
        newList.remove(user.getIdLong());
        TaskScheduler.async(() -> {
            Melijn.mySQL.removeUnverifiedUser(guild.getIdLong(), user.getIdLong());
            unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);
        });
        if (guild.getMember(user) != null && guild.getRoleById(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(guild.getIdLong())) != null) {
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                guild.getController().removeSingleRoleFromMember(guild.getMember(user), guild.getRoleById(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(guild.getIdLong()))).reason("verified user").queue();
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

    private static String variableFormat(String s, Guild guild, User user) {
        return s.replaceAll("%USER%", "<@" + user.getIdLong() + ">")
                .replaceAll("%USERNAME%", user.getName() + "#" + user.getDiscriminator())
                .replaceAll("%GUILDNAME%", guild.getName())
                .replaceAll("%SERVERNAME%", guild.getName())
                .replaceAll("%JOINPOSITION%", String.valueOf(guild.getMemberCache().size()));
    }
}
