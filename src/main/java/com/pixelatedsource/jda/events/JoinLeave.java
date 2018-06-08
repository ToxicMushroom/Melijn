package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.commands.management.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JoinLeave extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (SetJoinLeaveChannelCommand.welcomeChannels.containsKey(guild.getIdLong()) && SetJoinMessageCommand.joinMessages.containsKey(guild.getIdLong())) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannels.get(guild.getIdLong()));
            if (welcomeChannel != null && guild.getSelfMember().hasPermission(welcomeChannel, Permission.MESSAGE_WRITE))
                welcomeChannel.sendMessage(SetJoinMessageCommand.joinMessages.get(guild.getIdLong())
                        .replaceAll("%USER%", "<@" + joinedUser.getIdLong() + ">")
                        .replaceAll("%USERNAME%", joinedUser.getName() + "#" + joinedUser.getDiscriminator())
                        .replaceAll("%GUILDNAME%", guild.getName())
                        .replaceAll("%SERVERNAME%", guild.getName())).queue();
        }
        if (guild.getSelfMember().getRoles().size() > 0) {
            if (SetJoinRoleCommand.joinRoles.containsKey(guild.getIdLong())) {
                Role joinRole = guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()));
                if (joinRole != null && joinRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                    guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()))).queue();
            }
            new Thread(() -> {
                if (PixelSniper.mySQL.isUserMuted(joinedUser.getIdLong(), guild.getIdLong()) && SetMuteRoleCommand.muteRoles.containsKey(guild.getIdLong())) {
                    Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getIdLong()));
                    if (muteRole != null && muteRole.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
                        guild.getController().addSingleRoleToMember(event.getMember(), muteRole).queue();
                }
            });
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
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
}
