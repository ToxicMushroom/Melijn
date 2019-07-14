package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.Permission;

import java.util.List;

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
                String description = "MusicChannel:** " + idToChannelMention(event.getVariables().musicChannelCache.get(guildId)) +
                                "\n**StreamUrl:** " + stringToString(event.getMySQL().getStreamUrl(guildId), false) +
                                "\n**StreamerMode:** " + (event.getVariables().streamerModeCache.get(guildId) ? "on" : "off") +
                                "\n" +
                                "\n**MuteRole:** " + idToRoleMention(event.getVariables().muteRoleCache.get(guildId)) +
                                "\n**JoinRole:** " + idToRoleMention(event.getVariables().joinRoleCache.get(guildId)) +
                                "\n**UnverifiedRole:** " + idToRoleMention(event.getVariables().unverifiedRoleCache.get(guildId)) +
                                "\n" +
                                "\n**VerificationChannel:** " + idToChannelMention(event.getVariables().verificationChannelsCache.get(guildId)) +
                                "\n**JoinChannel:** " + idToChannelMention(event.getVariables().joinChannelCache.get(guildId)) +
                                "\n**LeaveChannel:** " + idToChannelMention(event.getVariables().leaveChannelCache.get(guildId)) +
                                "\n**SelfRoleChannel:** " + idToChannelMention(event.getVariables().selfRolesChannels.get(guildId)) +
                                "\n**BanLogChannel:** " + idToChannelMention(event.getVariables().banLogChannelCache.get(guildId)) +
                                "\n**MuteLogChannel:** " + idToChannelMention(event.getVariables().muteLogChannelCache.get(guildId)) +
                                "\n**KickLogChannel:** " + idToChannelMention(event.getVariables().kickLogChannelCache.get(guildId)) +
                                "\n**WarnLogChannel:** " + idToChannelMention(event.getVariables().warnLogChannelCache.get(guildId)) +
                                "\n**ODMLogChannel:** " + idToChannelMention(event.getVariables().odmLogChannelCache.get(guildId)) +
                                "\n**SDMLogChannel:** " + idToChannelMention(event.getVariables().sdmLogChannelCache.get(guildId)) +
                                "\n**PMLogChannel:** " + idToChannelMention(event.getVariables().pmLogChannelCache.get(guildId)) +
                                "\n**FMLogChannel:** " + idToChannelMention(event.getVariables().fmLogChannelCache.get(guildId)) +
                                "\n**EMLogChannel:** " + idToChannelMention(event.getVariables().emLogChannelCache.get(guildId)) +
                                "\n**ReactionChannel:** " + idToChannelMention(event.getVariables().reactionLogChannelCache.get(guildId)) +
                                "\n**AttachmentChannel:** " + idToChannelMention(event.getVariables().attachmentLogChannelCache.get(guildId)) +
                                "\n" +
                                "\n**JoinMessage: " + stringToString(event.getVariables().joinMessages.get(guildId).replaceAll("`", "´"), true) +
                                "\n\nLeaveMessage: " + stringToString(event.getVariables().leaveMessages.get(guildId).replaceAll("`", "´"), true) +
                                "\n\nVerificationCode:** " + stringToString(event.getVariables().verificationCodeCache.get(guildId), false) +
                                "\n**VerificationType:** " + stringToString(event.getVariables().verificationTypes.get(guildId).name(), false) +
                                "\n**VerificationThreshold:** " + event.getVariables().verificationThresholdCache.get(guildId) +
                        "\n**Prefix:** " + event.getVariables().prefixes.get(guildId) + "**";
                if (description.length() > 2048) {
                    List<String> parts = event.getMessageHelper().getSplitMessage(description, 0);
                    int i = 1;
                    for (String part : parts) {
                        event.reply(new Embedder(event.getVariables(), event.getGuild())
                                .setTitle("Server settings part #" + i++)
                                .setDescription(part)
                                .build());
                    }
                } else {
                    event.reply(new Embedder(event.getVariables(), event.getGuild())
                            .setTitle("Server settings")
                            .setDescription(description)
                            .build());
                }
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
