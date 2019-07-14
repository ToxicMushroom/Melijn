package me.melijn.jda.commands.management;

import com.github.benmanes.caffeine.cache.LoadingCache;
import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import static me.melijn.jda.Melijn.PREFIX;


public class SetLogChannelCommand extends Command {


    public SetLogChannelCommand() {
        this.commandName = "setLogChannel";
        this.description = "Main management command to configure where logs need to go";
        this.usage = PREFIX + commandName + " <type> [TextChannel | null]";
        this.extra = "Types: all, ban, mute, warn, kick, music, self-deleted-messages, other-deleted-messages, purged-messages, filtered-messages, edited-messages, reactions";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 80;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 1) {
                ChannelType chosenType;
                long id = event.getHelpers().getTextChannelByArgsN(event, args[1]);
                switch (args[0].toLowerCase()) {
                    case "all":
                        if (id == -1) {
                            event.sendUsage(this, event);
                        } else if (id == 0L) {
                            event.async(() -> {
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.BAN_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.MUTE_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.KICK_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.WARN_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.EM_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.REACTION_LOG);
                                event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.ATTACHMENT_LOG);
                                event.getVariables().banLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().muteLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().kickLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().warnLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().musicLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().sdmLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().odmLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().pmLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().fmLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().emLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().reactionLogChannelCache.invalidate(guild.getIdLong());
                                event.getVariables().attachmentLogChannelCache.invalidate(guild.getIdLong());
                            });
                            event.reply("All LogChannels have been changed to nothing by **" + event.getFullAuthorName() + "**");
                        } else {
                            event.async(() -> {
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.BAN_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.MUTE_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.KICK_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.WARN_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.SDM_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.ODM_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.PM_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.FM_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.EM_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.REACTION_LOG);
                                event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.ATTACHMENT_LOG);
                                event.getVariables().banLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().muteLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().kickLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().warnLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().musicLogChannelCache.put(guild.getIdLong(), id);
                                event.getVariables().sdmLogChannelCache.put(guild.getIdLong(), id);
                                event.getVariables().odmLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().pmLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().fmLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().emLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().reactionLogChannelCache.put(event.getGuildId(), id);
                                event.getVariables().attachmentLogChannelCache.put(event.getGuildId(), id);
                            });
                            event.reply("All LogChannels have been changed to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                        }
                        return;
                    case "ban":
                    case "bans":
                        chosenType = ChannelType.BAN_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().banLogChannelCache);
                        break;
                    case "mutes":
                    case "mute":
                        chosenType = ChannelType.MUTE_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().muteLogChannelCache);
                        break;
                    case "kicks":
                    case "kick":
                        chosenType = ChannelType.KICK_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().kickLogChannelCache);
                        break;
                    case "warns":
                    case "warn":
                        chosenType = ChannelType.WARN_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().warnLogChannelCache);
                        break;
                    case "songs":
                    case "music":
                        chosenType = ChannelType.MUSIC_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().musicLogChannelCache);
                        break;
                    case "sdm":
                    case "s-d-m":
                    case "self-deleted-messages":
                        chosenType = ChannelType.SDM_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().sdmLogChannelCache);
                        break;
                    case "odm":
                    case "o-d-m":
                    case "other-deleted-messages":
                        chosenType = ChannelType.ODM_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().odmLogChannelCache);
                        break;
                    case "pm":
                    case "p-m":
                    case "purged-messages":
                        chosenType = ChannelType.PM_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().pmLogChannelCache);
                        break;
                    case "fm":
                    case "f-m":
                    case "filtered-messages":
                        chosenType = ChannelType.FM_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().fmLogChannelCache);
                        break;
                    case "em":
                    case "e-m":
                    case "edited-messages":
                        chosenType = ChannelType.EM_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().emLogChannelCache);
                        break;
                    case "reaction":
                    case "reactions":
                        chosenType = ChannelType.REACTION_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().reactionLogChannelCache);
                        break;
                    case "attachment":
                    case "attachments":
                        chosenType = ChannelType.ATTACHMENT_LOG;
                        setLogChannel(event, chosenType, id, event.getVariables().attachmentLogChannelCache);
                        break;
                    default:
                        event.sendUsage(this, event);
                        break;
                }
            } else if (args.length == 1 && !args[0].isEmpty()) {
                switch (args[0].toLowerCase()) {
                    case "all":
                        StringBuilder builder = new StringBuilder();
                        TextChannel banChannel = guild.getTextChannelById(event.getVariables().banLogChannelCache.get(guild.getIdLong()));
                        TextChannel muteChannel = guild.getTextChannelById(event.getVariables().muteLogChannelCache.get(guild.getIdLong()));
                        TextChannel kickChannel = guild.getTextChannelById(event.getVariables().kickLogChannelCache.get(guild.getIdLong()));
                        TextChannel warnChannel = guild.getTextChannelById(event.getVariables().warnLogChannelCache.get(guild.getIdLong()));
                        TextChannel sdmChannel = guild.getTextChannelById(event.getVariables().sdmLogChannelCache.get(guild.getIdLong()));
                        TextChannel odmChannel = guild.getTextChannelById(event.getVariables().odmLogChannelCache.get(guild.getIdLong()));
                        TextChannel pmChannel = guild.getTextChannelById(event.getVariables().pmLogChannelCache.get(guild.getIdLong()));
                        TextChannel fmChannel = guild.getTextChannelById(event.getVariables().fmLogChannelCache.get(guild.getIdLong()));
                        TextChannel musicChannel = guild.getTextChannelById(event.getVariables().musicLogChannelCache.get(guild.getIdLong()));
                        TextChannel emChannel = guild.getTextChannelById(event.getVariables().emLogChannelCache.get(guild.getIdLong()));
                        TextChannel reactionChannel = guild.getTextChannelById(event.getVariables().reactionLogChannelCache.get(guild.getIdLong()));
                        TextChannel attachmentChannel = guild.getTextChannelById(event.getVariables().attachmentLogChannelCache.get(guild.getIdLong()));

                        builder.append("**Log Channels :clipboard:**\n")
                                .append("  Bans: ").append(banChannel == null ? "unset" : banChannel.getAsMention()).append("\n")
                                .append("  Mutes: ").append(muteChannel == null ? "unset" : muteChannel.getAsMention()).append("\n")
                                .append("  Kicks: ").append(kickChannel == null ? "unset" : kickChannel.getAsMention()).append("\n")
                                .append("  Warns: ").append(warnChannel == null ? "unset" : warnChannel.getAsMention()).append("\n")
                                .append("  SelfDeleteMessages: ").append(sdmChannel == null ? "unset" : sdmChannel.getAsMention()).append("\n")
                                .append("  OtherDeleteMessages: ").append(odmChannel == null ? "unset" : odmChannel.getAsMention()).append("\n")
                                .append("  PurgedMessages: ").append(pmChannel == null ? "unset" : pmChannel.getAsMention()).append("\n")
                                .append("  FilteredMessages: ").append(fmChannel == null ? "unset" : fmChannel.getAsMention()).append("\n")
                                .append("  Music: ").append(musicChannel == null ? "unset" : musicChannel.getAsMention()).append("\n")
                                .append("  MessageEdits: ").append(emChannel == null ? "unset" : emChannel.getAsMention()).append("\n")
                                .append("  Reactions: ").append(reactionChannel == null ? "unset" : reactionChannel.getAsMention()).append("\n")
                                .append("  Attachments: ").append(attachmentChannel == null ? "unset" : attachmentChannel.getAsMention()).append("\n");
                        event.reply(builder.toString());
                        break;

                    case "ban":
                    case "bans":
                        banChannel = guild.getTextChannelById(event.getVariables().banLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Ban Log \uD83D\uDD28**\n" + (banChannel == null ? "unset" : banChannel.getAsMention()));
                        break;

                    case "mute":
                    case "mutes":
                        muteChannel = guild.getTextChannelById(event.getVariables().muteLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Mute Log \uD83E\uDD10**\n" + (muteChannel == null ? "unset" : muteChannel.getAsMention()));
                        break;

                    case "kick":
                    case "kicks":
                        kickChannel = guild.getTextChannelById(event.getVariables().kickLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Kick Log \uD83E\uDD1C\uD83D\uDCA2**\n" + (kickChannel == null ? "unset" : kickChannel.getAsMention()));
                        break;

                    case "warn":
                    case "warns":
                        warnChannel = guild.getTextChannelById(event.getVariables().warnLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Warn Log \u203C**\n" + (warnChannel == null ? "unset" : warnChannel.getAsMention()));
                        break;

                    case "music":
                        musicChannel = guild.getTextChannelById(event.getVariables().musicLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Music Log \uD83C\uDFB5**\n" + (musicChannel == null ? "unset" : musicChannel.getAsMention()));
                        break;

                    case "sdm":
                    case "s-d-m":
                    case "self-deleted-messages":
                        sdmChannel = guild.getTextChannelById(event.getVariables().sdmLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Self Deleted Log \uD83D\uDC64**\n" + (sdmChannel == null ? "unset" : sdmChannel.getAsMention()));
                        break;

                    case "odm":
                    case "o-d-m":
                    case "other-deleted-messages":
                        odmChannel = guild.getTextChannelById(event.getVariables().odmLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Other Deleted Log \uD83D\uDC65**\n" +
                                (odmChannel == null ? "unset" : odmChannel.getAsMention()));

                        break;
                    case "pm":
                    case "p-m":
                    case "purged-messages":
                        pmChannel = guild.getTextChannelById(event.getVariables().pmLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Purge Log \u267B**\n" + (pmChannel == null ? "unset" : pmChannel.getAsMention()));

                        break;
                    case "fm":
                    case "f-m":
                    case "filtered-messages":
                        fmChannel = guild.getTextChannelById(event.getVariables().fmLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Filter Log \uD83D\uDEB3**\n" + (fmChannel == null ? "unset" : fmChannel.getAsMention()));
                        break;
                    case "em":
                    case "e-m":
                    case "edited-messages":
                        emChannel = guild.getTextChannelById(event.getVariables().emLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Filter Log \uD83D\uDEB3**\n" + (emChannel == null ? "unset" : emChannel.getAsMention()));
                        break;
                    case "reaction":
                    case "reactions":
                        reactionChannel = guild.getTextChannelById(event.getVariables().reactionLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Reaction Log \uD83D\uDC4C**\n" + (reactionChannel == null ? "unset" : reactionChannel.getAsMention()));
                        break;
                    case "attachment":
                    case "attachments":
                        attachmentChannel = guild.getTextChannelById(event.getVariables().attachmentLogChannelCache.get(guild.getIdLong()));
                        event.reply("**Reaction Log \uD83D\uDC4C**\n" + (attachmentChannel == null ? "unset" : attachmentChannel.getAsMention()));
                        break;
                    default:
                        event.sendUsage(this, event);
                        break;
                }
            } else {
                event.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void setLogChannel(CommandEvent event, ChannelType chosenType, long channelId, LoadingCache<Long, Long> toUse) {
        Guild guild = event.getGuild();
        if (channelId == -1) {
            event.sendUsage(this, event);
        } else if (channelId == 0L) {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (toUse.get(guild.getIdLong()) != -1 ? "<#" + toUse.get(guild.getIdLong()) + ">" : "nothing") + " to nothing by **" + event.getFullAuthorName() + "**");
            event.async(() -> {
                event.getMySQL().removeChannel(guild.getIdLong(), chosenType);
                toUse.invalidate(guild.getIdLong());
            });
        } else {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (toUse.get(guild.getIdLong()) != -1 ? "<#" + toUse.get(guild.getIdLong()) + ">" : "nothing") + " to <#" + channelId + "> by **" + event.getFullAuthorName() + "**");
            event.async(() -> {
                event.getMySQL().setChannel(guild.getIdLong(), channelId, chosenType);
                toUse.put(guild.getIdLong(), channelId);
            });
        }
    }
}
