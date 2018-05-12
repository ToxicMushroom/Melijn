package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.commands.management.*;
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
        if (SetJoinLeaveChannelCommand.welcomChannels.getOrDefault(guild.getId(),"a").matches("\\d+") && SetJoinMessageCommand.joinMessages.containsKey(guild.getId())) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomChannels.get(guild.getId()));
            welcomeChannel.sendMessage(SetJoinMessageCommand.joinMessages.get(guild.getId())
                    .replaceAll("%USER%", "<@" + joinedUser.getId() + ">")
                    .replaceAll("%USERNAME%", joinedUser.getName() + "#" + joinedUser.getDiscriminator())).queue();
        }
        if (SetJoinRoleCommand.joinRoles.containsKey(guild.getId()) &&
                SetJoinRoleCommand.joinRoles.get(guild.getId()).matches("\\d+") &&
                guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getId())) != null)
            guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetJoinRoleCommand.joinRoles.get(guild.getId()))).queue();
        new Thread(() -> {
            if (PixelSniper.mySQL.isUserMuted(joinedUser, guild) && SetMuteRoleCommand.muteRoles.getOrDefault(guild.getId(), "null").matches("\\d+") && guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getId())) != null) guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(SetMuteRoleCommand.muteRoles.get(guild.getId()))).queue();
        });
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (SetJoinLeaveChannelCommand.welcomChannels.getOrDefault(guild.getId(),"a").matches("\\d+") && SetLeaveMessageCommand.leaveMessages.containsKey(guild.getId())) {
            TextChannel welcomeChannel = guild.getTextChannelById(SetJoinLeaveChannelCommand.welcomChannels.get(guild.getId()));
            welcomeChannel.sendMessage(SetLeaveMessageCommand.leaveMessages.get(guild.getId())
                    .replaceAll("%USER%", "<@" + leftUser.getId() + ">")
                    .replaceAll("%USERNAME%", leftUser.getName() + "#" + leftUser.getDiscriminator())).queue();
        }
    }
}
