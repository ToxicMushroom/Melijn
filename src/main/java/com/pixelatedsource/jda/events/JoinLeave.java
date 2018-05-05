package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.MessageType;
import com.pixelatedsource.jda.blub.RoleType;
import com.pixelatedsource.jda.db.MySQL;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JoinLeave extends ListenerAdapter {

    private MySQL mySQL = PixelSniper.mySQL;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (mySQL.getChannelId(guild, ChannelType.WELCOME) != null && mySQL.getMessage(guild, MessageType.JOIN) != null) {
            TextChannel welcomeChannel = guild.getTextChannelById(mySQL.getChannelId(guild, ChannelType.WELCOME));
            welcomeChannel.sendMessage(mySQL.getMessage(guild, MessageType.JOIN)
                    .replaceAll("%USER%", "<@" + joinedUser.getId() + ">")
                    .replaceAll("%USERNAME%", joinedUser.getName() + "#" + joinedUser.getDiscriminator())).queue();
        }
        if (mySQL.getRoleId(guild, RoleType.JOIN) != null && !mySQL.getRoleId(guild, RoleType.JOIN).equalsIgnoreCase("null") && guild.getRoleById(mySQL.getRoleId(guild, RoleType.JOIN)) != null)
            guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(mySQL.getRoleId(guild, RoleType.JOIN))).queue();
        if (mySQL.isUserMuted(joinedUser, guild) && mySQL.getRoleId(guild, RoleType.MUTE) != null)
            guild.getController().addSingleRoleToMember(event.getMember(), guild.getRoleById(mySQL.getRoleId(guild, RoleType.MUTE))).queue();
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (mySQL.getChannelId(guild, ChannelType.WELCOME) != null && mySQL.getMessage(guild, MessageType.LEAVE) != null) {
            TextChannel welcomeChannel = guild.getTextChannelById(mySQL.getChannelId(guild, ChannelType.WELCOME));
            welcomeChannel.sendMessage(mySQL.getMessage(guild, MessageType.LEAVE)
                    .replaceAll("%USER%", "<@" + leftUser.getId() + ">")
                    .replaceAll("%USERNAME%", leftUser.getName() + "#" + leftUser.getDiscriminator())).queue();
        }
    }
}
