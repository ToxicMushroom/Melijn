package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.commands.management.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
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
            if (welcomeChannel != null && Helpers.checkChannelPermission(guild.getSelfMember(), welcomeChannel, Permission.MESSAGE_WRITE))
                welcomeChannel.sendMessage(SetJoinMessageCommand.joinMessages.get(guild.getIdLong())
                        .replaceAll("%USER%", "<@" + joinedUser.getIdLong() + ">")
                        .replaceAll("%USERNAME%", joinedUser.getName() + "#" + joinedUser.getDiscriminator())).queue();
        }
        if (SetJoinRoleCommand.joinRoles.containsKey(guild.getIdLong()) &&
                guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong())) != null)
            guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getIdLong()))).queue();
        new Thread(() -> {
            if (PixelSniper.mySQL.isUserMuted(joinedUser.getIdLong(), guild.getIdLong()) && SetMuteRoleCommand.muteRoles.containsKey(guild.getIdLong()) && guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getIdLong())) != null)
                guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getIdLong()))).queue();
        });
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (SetJoinLeaveChannelCommand.welcomeChannels.containsKey(guild.getIdLong()) && SetLeaveMessageCommand.leaveMessages.containsKey(guild.getIdLong())) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomeChannels.get(guild.getIdLong()));
            if (welcomeChannel != null && Helpers.checkChannelPermission(guild.getSelfMember(), welcomeChannel, Permission.MESSAGE_WRITE))
            welcomeChannel.sendMessage(SetLeaveMessageCommand.leaveMessages.get(guild.getIdLong())
                    .replaceAll("%USER%", "<@" + leftUser.getId() + ">")
                    .replaceAll("%USERNAME%", leftUser.getName() + "#" + leftUser.getDiscriminator())).queue();
        }
    }
}
