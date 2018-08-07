package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.commands.developer.EvalCommand;
import com.pixelatedsource.jda.commands.management.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class JoinLeave extends ListenerAdapter {

    public static HashMap<Long, ArrayList<Long>> unVerifiedGuildMembers = PixelSniper.mySQL.getUnverifiedUserMap();

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (SetVerificationChannel.verificationChannels.containsKey(guild.getIdLong())) {
            TextChannel verificationChannel = guild.getTextChannelById(SetVerificationChannel.verificationChannels.get(guild.getIdLong()));
            if (verificationChannel != null) {
                ArrayList<Long> newList = unVerifiedGuildMembers.getOrDefault(guild.getIdLong(), new ArrayList<>());
                newList.add(joinedUser.getIdLong());
                if (unVerifiedGuildMembers.replace(guild.getIdLong(), newList) == null) {
                    unVerifiedGuildMembers.put(guild.getIdLong(), newList);
                }
                PixelSniper.mySQL.addUnverifiedUser(guild.getIdLong(), joinedUser.getIdLong());
                if (SetUnverifiedRole.unverifiedRoles.containsKey(guild.getIdLong()) && guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong())) != null)
                    guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong()))).reason("unverified user").queue();
            } else {
                SetVerificationChannel.verificationChannels.remove(event.getGuild().getIdLong());
                PixelSniper.MAIN_THREAD.submit(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION));
            }
        } else {
            joinCode(guild, joinedUser);
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (unVerifiedGuildMembers.get(guild.getIdLong()) == null || !unVerifiedGuildMembers.get(guild.getIdLong()).contains(leftUser.getIdLong())) {
            if (SetJoinLeaveChannelCommand.welcomeChannels.containsKey(guild.getIdLong()) && SetLeaveMessageCommand.leaveMessages.containsKey(guild.getIdLong())) {
                TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannels.get(guild.getIdLong()));
                if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                    welcomeChannel.sendMessage(SetLeaveMessageCommand.leaveMessages.get(guild.getIdLong())
                            .replaceAll("%USER%", "<@" + leftUser.getId() + ">")
                            .replaceAll("%USERNAME%", leftUser.getName() + "#" + leftUser.getDiscriminator())
                            .replaceAll("%GUILDNAME%", guild.getName())
                            .replaceAll("%SERVERNAME%", guild.getName())).queue();
            }
        }
        removeUnverified(guild, leftUser);
    }

    public static void verify(Guild guild, User user) {
        removeUnverified(guild, user);
        joinCode(guild, user);
    }

    private static void joinCode(Guild guild, User user) {
        if (SetJoinLeaveChannelCommand.welcomeChannels.containsKey(guild.getIdLong()) && SetJoinMessageCommand.joinMessages.containsKey(guild.getIdLong())) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannels.get(guild.getIdLong()));
            if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                welcomeChannel.sendMessage(SetJoinMessageCommand.joinMessages.get(guild.getIdLong())
                        .replaceAll("%USER%", "<@" + user.getIdLong() + ">")
                        .replaceAll("%USERNAME%", user.getName() + "#" + user.getDiscriminator())
                        .replaceAll("%GUILDNAME%", guild.getName())
                        .replaceAll("%SERVERNAME%", guild.getName())).queue();
        }
        if (guild.getSelfMember().getRoles().size() > 0) {
            if (SetJoinRoleCommand.joinRoles.containsKey(guild.getIdLong())) {
                Role joinRole = guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()));
                if (joinRole != null && joinRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                    guild.getController().addSingleRoleToMember(guild.getMember(user), guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()))).queue();
            }
            PixelSniper.MAIN_THREAD.submit(() ->  {
                if (PixelSniper.mySQL.isUserMuted(user.getIdLong(), guild.getIdLong()) && SetMuteRoleCommand.muteRoles.containsKey(guild.getIdLong())) {
                    Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getIdLong()));
                    if (muteRole != null && muteRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                        guild.getController().addSingleRoleToMember(guild.getMember(user), muteRole).queue();
                }
            });
        }
    }

    private static void removeUnverified(Guild guild, User user) {
        if (unVerifiedGuildMembers.get(guild.getIdLong()) != null) {
            ArrayList<Long> newList = unVerifiedGuildMembers.get(guild.getIdLong());
            newList.remove(user.getIdLong());
            unVerifiedGuildMembers.replace(guild.getIdLong(), newList);
            PixelSniper.MAIN_THREAD.submit(() ->  PixelSniper.mySQL.removeUnverifiedUser(guild.getIdLong(), user.getIdLong()));
            if (guild.getMember(user) != null && SetUnverifiedRole.unverifiedRoles.containsKey(guild.getIdLong()) && guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong())) != null)
                guild.getController().removeSingleRoleFromMember(guild.getMember(user), guild.getRoleById(SetUnverifiedRole.unverifiedRoles.get(guild.getIdLong()))).reason("verified user").queue();
        }
    }
}
