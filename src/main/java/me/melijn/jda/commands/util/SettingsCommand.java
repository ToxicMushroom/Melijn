package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class SettingsCommand extends Command {

    public SettingsCommand() {
        this.commandName = "settings";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"guildSettings", "serverSettings"};
        this.description = "Shows all the settings of melijn and their configured value";
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.needs = new Need[]{Need.GUILD};
        this.id = 89;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            event.async(() -> {
                long guildId = event.getGuild().getIdLong();
                event.reply(new Embedder(event.getVariables(), event.getGuild())
                        .setTitle("Server settings")
                        .setDescription("MusicChannel:** " + idToChannelMention(event.getVariables().musicChannelCache.getUnchecked(guildId)) +
                                "\n**StreamUrl:** " + stringToString(event.getMySQL().getStreamUrl(guildId), false) +
                                "\n**StreamerMode:** " + (event.getVariables().streamerModeCache.getUnchecked(guildId) ? "on" : "off") +
                                "\n" +
                                "\n**MuteRole:** " + idToRoleMention(event.getVariables().muteRoleCache.getUnchecked(guildId)) +
                                "\n**JoinRole:** " + idToRoleMention(event.getVariables().joinRoleCache.getUnchecked(guildId)) +
                                "\n**UnverifiedRole:** " + idToRoleMention(event.getVariables().unverifiedRoleCache.getUnchecked(guildId)) +
                                "\n" +
                                "\n**VerificationChannel:** " + idToChannelMention(event.getVariables().verificationChannelsCache.getUnchecked(guildId)) +
                                "\n**WelcomeChannel:** " + idToChannelMention(event.getVariables().welcomeChannelCache.getUnchecked(guildId)) +
                                "\n**SelfRoleChannel:** " + idToChannelMention(event.getVariables().selfRolesChannels.getUnchecked(guildId)) +
                                "\n**BanLogChannel:** " + idToChannelMention(event.getVariables().banLogChannelCache.getUnchecked(guildId)) +
                                "\n**MuteLogChannel:** " + idToChannelMention(event.getVariables().muteLogChannelCache.getUnchecked(guildId)) +
                                "\n**KickLogChannel:** " + idToChannelMention(event.getVariables().kickLogChannelCache.getUnchecked(guildId)) +
                                "\n**WarnLogChannel:** " + idToChannelMention(event.getVariables().warnLogChannelCache.getUnchecked(guildId)) +
                                "\n**ODMLogChannel:** " + idToChannelMention(event.getVariables().odmLogChannelCache.getUnchecked(guildId)) +
                                "\n**SDMLogChannel:** " + idToChannelMention(event.getVariables().sdmLogChannelCache.getUnchecked(guildId)) +
                                "\n**PMLogChannel:** " + idToChannelMention(event.getVariables().pmLogChannelCache.getUnchecked(guildId)) +
                                "\n**FMLogChannel:** " + idToChannelMention(event.getVariables().fmLogChannelCache.getUnchecked(guildId)) +
                                "\n**EMLogChannel:** " + idToChannelMention(event.getVariables().emLogChannelCache.getUnchecked(guildId)) +
                                "\n**ReactionChannel:** " + idToChannelMention(event.getVariables().reactionLogChannelCache.getUnchecked(guildId)) +
                                "\n**AttachmentChannel:** " + idToChannelMention(event.getVariables().attachmentLogChannelCache.getUnchecked(guildId)) +
                                "\n" +
                                "\n**JoinMessage: " + stringToString(event.getVariables().joinMessages.getUnchecked(guildId).replaceAll("`", "´"), true) +
                                "\n\nLeaveMessage: " + stringToString(event.getVariables().leaveMessages.getUnchecked(guildId).replaceAll("`", "´"), true) +
                                "\n\nVerificationCode:** " + stringToString(event.getVariables().verificationCodeCache.getUnchecked(guildId), false) +
                                "\n**VerificationType:** " + stringToString(event.getVariables().verificationTypes.getUnchecked(guildId).name(), false) +
                                "\n**VerificationThreshold:** " + event.getVariables().verificationThresholdCache.getUnchecked(guildId) +
                                "\n**Prefix:** " + event.getVariables().prefixes.getUnchecked(guildId) + "**")
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
