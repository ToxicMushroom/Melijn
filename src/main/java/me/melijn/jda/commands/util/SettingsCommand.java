package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class SettingsCommand extends Command {

    public SettingsCommand() {
        this.commandName = "settings";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"guildSettings", "serverSettings"};
        this.description = "Shows you all the settings of melijn and their set value";
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            TaskScheduler.async(() ->
                    event.reply(new EmbedBuilder()
                            .setTitle("Server settings")
                            .setColor(Helpers.EmbedColor)
                            .setDescription("MusicChannel:** " + idToChannelMention(SetMusicChannelCommand.musicChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**StreamUrl:** " + stringToString(Melijn.mySQL.getStreamUrl(event.getGuild().getIdLong()), false) +
                                    "\n**StreamerMode:** " + (SetStreamerModeCommand.streamerModeCache.getUnchecked(event.getGuild().getIdLong()) ? "on" : "off") +
                                    "\n" +
                                    "\n**MuteRole:** " + idToRoleMention(SetMuteRoleCommand.muteRoleCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**JoinRole:** " + idToRoleMention(SetJoinRoleCommand.joinRoleCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**UnverifiedRole:** " + idToRoleMention(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n" +
                                    "\n**VerificationChannel:** " + idToChannelMention(SetVerificationChannel.verificationChannelsCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**WelcomeChannel:** " + idToChannelMention(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**BanLogChannel:** " + idToChannelMention(SetLogChannelCommand.banLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**MuteLogChannel:** " + idToChannelMention(SetLogChannelCommand.muteLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**KickLogChannel:** " + idToChannelMention(SetLogChannelCommand.kickLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**WarnLogChannel:** " + idToChannelMention(SetLogChannelCommand.warnLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**ODMLogChannel:** " + idToChannelMention(SetLogChannelCommand.odmLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**SDMLogChannel:** " + idToChannelMention(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**PMLogChannel:** " + idToChannelMention(SetLogChannelCommand.pmLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n**FMLogChannel:** " + idToChannelMention(SetLogChannelCommand.fmLogChannelCache.getUnchecked(event.getGuild().getIdLong())) +
                                    "\n" +
                                    "\n**JoinMessage: " + stringToString(SetJoinMessageCommand.joinMessages.getUnchecked(event.getGuild().getIdLong()).replaceAll("`", "´"), true) +
                                    "\n\nLeaveMessage: " + stringToString(SetLeaveMessageCommand.leaveMessages.getUnchecked(event.getGuild().getIdLong()).replaceAll("`", "´"), true) +
                                    "\n\nVerificationCode:** " + stringToString(SetVerificationCode.verificationCodeCache.getUnchecked(event.getGuild().getIdLong()), false) +
                                    "\n**VerificationThreshold:** " + SetVerificationThreshold.verificationThresholdCache.getUnchecked(event.getGuild().getIdLong()) +
                                    "\n**Prefix:** " + SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()) + "**")
                            .build())
            );
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private String idToChannelMention(long channelId) {
        if (channelId == -1L) {
            return "unset";
        } else {
            return "<#" + channelId + ">";
        }
    }

    private String idToRoleMention(long roleId) {
        if (roleId == -1L) {
            return "unset";
        } else {
            return "<@&" + roleId + ">";
        }
    }

    private String stringToString(String text, boolean encapsulate) {
        if (encapsulate)
        return text.equals("") ? "unset" : "```" + text + "```";
        else
            return text.equals("") ? "unset" : text;
    }
}
