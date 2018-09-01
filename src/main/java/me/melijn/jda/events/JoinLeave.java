package me.melijn.jda.events;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JoinLeave extends ListenerAdapter {

    public static final LoadingCache<Long, ArrayList<Long>> unVerifiedGuildMembersCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public ArrayList<Long> load(@NotNull Long key) {
                    return Melijn.mySQL.getUnverifiedMembers(key);
                }
            });


    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (SetVerificationChannel.verificationChannelsCache.getUnchecked(guild.getIdLong()) != -1) {
            TextChannel verificationChannel = guild.getTextChannelById(SetVerificationChannel.verificationChannelsCache.getUnchecked(guild.getIdLong()));
            if (verificationChannel != null) {
                ArrayList<Long> newList = unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
                newList.add(joinedUser.getIdLong());
                Melijn.mySQL.addUnverifiedUser(guild.getIdLong(), joinedUser.getIdLong());
                unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);
                if (SetUnverifiedRole.unverifiedRoles.containsKey(guild.getIdLong()) && guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong())) != null)
                    guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong()))).reason("unverified user").queue();
            } else {
                TaskScheduler.async(() -> {
                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION);
                    SetVerificationChannel.verificationChannelsCache.invalidate(guild.getIdLong());
                });
            }
        } else {
            try {
                joinCode(guild, joinedUser);
            } catch (ExecutionException ignore) { }
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong())) return;
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong()).contains(leftUser.getIdLong())) {
            TaskScheduler.async(() -> {
                String message = SetLeaveMessageCommand.leaveMessages.getUnchecked(guild.getIdLong());
                if (message != null && SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()) != -1) {
                    TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()));
                    if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                        welcomeChannel.sendMessage(variableFormat(message, guild, leftUser)).queue();
                }
            });
        }
        removeUnverified(guild, leftUser);
    }

    public static void verify(Guild guild, User user) {
        removeUnverified(guild, user);
        try {
            joinCode(guild, user);
        } catch (ExecutionException ignore) { }
    }

    private static void joinCode(Guild guild, User user) throws ExecutionException {
        if (SetJoinMessageCommand.joinMessages.get(guild.getIdLong()) != null && SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()) != -1) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guild.getIdLong()));
            if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                welcomeChannel.sendMessage(variableFormat(SetJoinMessageCommand.joinMessages.getUnchecked(guild.getIdLong()), guild, user)).queue();
        }
        if (guild.getSelfMember().getRoles().size() > 0) {
            if (SetJoinRoleCommand.joinRoles.containsKey(guild.getIdLong())) {
                Role joinRole = guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()));
                if (joinRole != null && joinRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                    guild.getController().addSingleRoleToMember(guild.getMember(user), guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()))).queue();
            }
            TaskScheduler.async(() -> {
                if (Melijn.mySQL.isUserMuted(user.getIdLong(), guild.getIdLong()) && SetMuteRoleCommand.muteRoles.containsKey(guild.getIdLong())) {
                    Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getIdLong()));
                    if (muteRole != null && muteRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                        guild.getController().addSingleRoleToMember(guild.getMember(user), muteRole).queue();
                }
            });
        }
    }

    private static void removeUnverified(Guild guild, User user) {
        ArrayList<Long> newList = unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
        newList.remove(user.getIdLong());
        TaskScheduler.async(() -> {
            Melijn.mySQL.removeUnverifiedUser(guild.getIdLong(), user.getIdLong());
            unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);
        });
        if (guild.getMember(user) != null && SetUnverifiedRole.unverifiedRoles.containsKey(guild.getIdLong()) && guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong())) != null)
            guild.getController().removeSingleRoleFromMember(guild.getMember(user), guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong()))).reason("verified user").queue();
    }

    private static String variableFormat(String s, Guild guild, User user) {
        return s.replaceAll("%USER%", guild.getMember(user).getAsMention())
                .replaceAll("%USERNAME%", user.getName() + "#" + user.getDiscriminator())
                .replaceAll("%GUILDNAME%", guild.getName())
                .replaceAll("%SERVERNAME%", guild.getName())
                .replaceAll("%JOINPOSITION%", String.valueOf(guild.getMembers().size()));
    }
}
