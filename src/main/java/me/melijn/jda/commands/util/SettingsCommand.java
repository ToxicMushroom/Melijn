package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.TaskScheduler;
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
            TaskScheduler.async(() -> {
                long guildId = event.getGuild().getIdLong();
                event.reply(new Embedder(event.getGuild())
                        .setTitle("Server settings")
                        .setDescription("MusicChannel:** " + idToChannelMention(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)) +
                                "\n**StreamUrl:** " + stringToString(Melijn.mySQL.getStreamUrl(guildId), false) +
                                "\n**StreamerMode:** " + (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) ? "on" : "off") +
                                "\n" +
                                "\n**MuteRole:** " + idToRoleMention(SetMuteRoleCommand.muteRoleCache.getUnchecked(guildId)) +
                                "\n**JoinRole:** " + idToRoleMention(SetJoinRoleCommand.joinRoleCache.getUnchecked(guildId)) +
                                "\n**UnverifiedRole:** " + idToRoleMention(SetUnverifiedRole.unverifiedRoleCache.getUnchecked(guildId)) +
                                "\n" +
                                "\n**VerificationChannel:** " + idToChannelMention(SetVerificationChannel.verificationChannelsCache.getUnchecked(guildId)) +
                                "\n**WelcomeChannel:** " + idToChannelMention(SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guildId)) +
                                "\n**SelfRoleChannel:** " + idToChannelMention(SetSelfRoleChannelCommand.selfRolesChannel.getUnchecked(guildId)) +
                                "\n**BanLogChannel:** " + idToChannelMention(SetLogChannelCommand.banLogChannelCache.getUnchecked(guildId)) +
                                "\n**MuteLogChannel:** " + idToChannelMention(SetLogChannelCommand.muteLogChannelCache.getUnchecked(guildId)) +
                                "\n**KickLogChannel:** " + idToChannelMention(SetLogChannelCommand.kickLogChannelCache.getUnchecked(guildId)) +
                                "\n**WarnLogChannel:** " + idToChannelMention(SetLogChannelCommand.warnLogChannelCache.getUnchecked(guildId)) +
                                "\n**ODMLogChannel:** " + idToChannelMention(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guildId)) +
                                "\n**SDMLogChannel:** " + idToChannelMention(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guildId)) +
                                "\n**PMLogChannel:** " + idToChannelMention(SetLogChannelCommand.pmLogChannelCache.getUnchecked(guildId)) +
                                "\n**FMLogChannel:** " + idToChannelMention(SetLogChannelCommand.fmLogChannelCache.getUnchecked(guildId)) +
                                "\n" +
                                "\n**JoinMessage: " + stringToString(SetJoinMessageCommand.joinMessages.getUnchecked(guildId).replaceAll("`", "´"), true) +
                                "\n\nLeaveMessage: " + stringToString(SetLeaveMessageCommand.leaveMessages.getUnchecked(guildId).replaceAll("`", "´"), true) +
                                "\n\nVerificationCode:** " + stringToString(SetVerificationCode.verificationCodeCache.getUnchecked(guildId), false) +
                                "\n**VerificationThreshold:** " + SetVerificationThreshold.verificationThresholdCache.getUnchecked(guildId) +
                                "\n**Prefix:** " + SetPrefixCommand.prefixes.getUnchecked(guildId) + "**")
                        .build());
                    }
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
        return "".equals(text) ? "unset" : "```" + text + "```";
        else
            return "".equals(text) ? "unset" : text;
    }
}
