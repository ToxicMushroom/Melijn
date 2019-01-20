package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetLogChannelCommand extends Command {

    public static final LoadingCache<Long, Long> banLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.BAN_LOG);
                }
            });
    public static final LoadingCache<Long, Long> muteLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.MUTE_LOG);
                }
            });
    public static final LoadingCache<Long, Long> kickLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.KICK_LOG);
                }
            });
    public static final LoadingCache<Long, Long> warnLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.WARN_LOG);
                }
            });
    public static final LoadingCache<Long, Long> musicLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.MUSIC_LOG);
                }
            });
    public static final LoadingCache<Long, Long> sdmLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.SDM_LOG);
                }
            });
    public static final LoadingCache<Long, Long> emLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.EM_LOG);
                }
            });
    public static final LoadingCache<Long, Long> odmLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.ODM_LOG);
                }
            });
    public static final LoadingCache<Long, Long> pmLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.PM_LOG);
                }
            });
    public static final LoadingCache<Long, Long> fmLogChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.FM_LOG);
                }
            });

    public SetLogChannelCommand() {
        this.commandName = "setLogChannel";
        this.description = "Main management command to configure where logs need to go";
        this.usage = PREFIX + commandName + " <type> [TextChannel | null]";
        this.extra = "Types: all, ban, mute, warn, kick, music, self-deleted-messages, other-deleted-messages, purged-messages, filtered-messages";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 80;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 1) {
                ChannelType chosenType;
                long id = Helpers.getTextChannelByArgsN(event, args[1]);
                switch (args[0].toLowerCase()) {
                    case "all":
                        if (id == -1) {
                            MessageHelper.sendUsage(this, event);
                        } else if (id == 0L) {
                            TaskScheduler.async(() -> {
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.BAN_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUTE_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.KICK_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.WARN_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                                Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.EM_LOG);
                                banLogChannelCache.invalidate(guild.getIdLong());
                                muteLogChannelCache.invalidate(guild.getIdLong());
                                kickLogChannelCache.invalidate(guild.getIdLong());
                                warnLogChannelCache.invalidate(guild.getIdLong());
                                musicLogChannelCache.invalidate(guild.getIdLong());
                                sdmLogChannelCache.invalidate(guild.getIdLong());
                                odmLogChannelCache.invalidate(guild.getIdLong());
                                pmLogChannelCache.invalidate(guild.getIdLong());
                                fmLogChannelCache.invalidate(guild.getIdLong());
                                emLogChannelCache.invalidate(guild.getIdLong());
                            });
                            event.reply("All LogChannels have been changed to nothing by **" + event.getFullAuthorName() + "**");
                        } else {
                            TaskScheduler.async(() -> {
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.BAN_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUTE_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.KICK_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WARN_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.SDM_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.ODM_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.PM_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.FM_LOG);
                                Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.EM_LOG);
                                banLogChannelCache.put(event.getGuildId(), id);
                                muteLogChannelCache.put(event.getGuildId(), id);
                                kickLogChannelCache.put(event.getGuildId(), id);
                                warnLogChannelCache.put(event.getGuildId(), id);
                                musicLogChannelCache.put(guild.getIdLong(), id);
                                sdmLogChannelCache.put(guild.getIdLong(), id);
                                odmLogChannelCache.put(event.getGuildId(), id);
                                pmLogChannelCache.put(event.getGuildId(), id);
                                fmLogChannelCache.put(event.getGuildId(), id);
                                emLogChannelCache.put(event.getGuildId(), id);
                            });
                            event.reply("All LogChannels have been changed to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                        }
                        return;
                    case "ban":
                    case "bans":
                        chosenType = ChannelType.BAN_LOG;
                        setLogChannel(event, chosenType, id, banLogChannelCache);
                        break;
                    case "mutes":
                    case "mute":
                        chosenType = ChannelType.MUTE_LOG;
                        setLogChannel(event, chosenType, id, muteLogChannelCache);
                        break;
                    case "kicks":
                    case "kick":
                        chosenType = ChannelType.KICK_LOG;
                        setLogChannel(event, chosenType, id, kickLogChannelCache);
                        break;
                    case "warns":
                    case "warn":
                        chosenType = ChannelType.WARN_LOG;
                        setLogChannel(event, chosenType, id, warnLogChannelCache);
                        break;
                    case "songs":
                    case "music":
                        chosenType = ChannelType.MUSIC_LOG;
                        setLogChannel(event, chosenType, id, musicLogChannelCache);
                        break;
                    case "sdm":
                    case "s-d-m":
                    case "self-deleted-messages":
                        chosenType = ChannelType.SDM_LOG;
                        setLogChannel(event, chosenType, id, sdmLogChannelCache);
                        break;
                    case "odm":
                    case "o-d-m":
                    case "other-deleted-messages":
                        chosenType = ChannelType.ODM_LOG;
                        setLogChannel(event, chosenType, id, odmLogChannelCache);
                        break;
                    case "pm":
                    case "p-m":
                    case "purged-messages":
                        chosenType = ChannelType.PM_LOG;
                        setLogChannel(event, chosenType, id, pmLogChannelCache);
                        break;
                    case "fm":
                    case "f-m":
                    case "filtered-messages":
                        chosenType = ChannelType.FM_LOG;
                        setLogChannel(event, chosenType, id, fmLogChannelCache);
                        break;
                    case "em":
                    case "e-m":
                    case "edited-messages":
                        chosenType = ChannelType.EM_LOG;
                        setLogChannel(event, chosenType, id, emLogChannelCache);
                        break;
                    default:
                        MessageHelper.sendUsage(this, event);
                        break;
                }
            } else if (args.length == 1 && !args[0].isEmpty()) {
                switch (args[0].toLowerCase()) {
                    case "all":
                        StringBuilder builder = new StringBuilder();
                        TextChannel banChannel = guild.getTextChannelById(banLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel muteChannel = guild.getTextChannelById(muteLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel kickChannel = guild.getTextChannelById(kickLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel warnChannel = guild.getTextChannelById(warnLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel sdmChannel = guild.getTextChannelById(sdmLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel odmChannel = guild.getTextChannelById(odmLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel pmChannel = guild.getTextChannelById(pmLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel fmChannel = guild.getTextChannelById(fmLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel musicChannel = guild.getTextChannelById(musicLogChannelCache.getUnchecked(guild.getIdLong()));
                        TextChannel emChannel = guild.getTextChannelById(emLogChannelCache.getUnchecked(guild.getIdLong()));

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
                                .append("  MessageEdits: ").append(emChannel == null ? "unset" : emChannel.getAsMention()).append("\n");
                        event.reply(builder.toString());
                        break;

                    case "ban":
                    case "bans":
                        banChannel = guild.getTextChannelById(banLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Ban Log \uD83D\uDD28**\n" + (banChannel == null ? "unset" : banChannel.getAsMention()));
                        break;

                    case "mute":
                    case "mutes":
                        muteChannel = guild.getTextChannelById(muteLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Mute Log \uD83E\uDD10**\n" + (muteChannel == null ? "unset" : muteChannel.getAsMention()));
                        break;

                    case "kick":
                    case "kicks":
                        kickChannel = guild.getTextChannelById(kickLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Kick Log \uD83E\uDD1C\uD83D\uDCA2**\n" + (kickChannel == null ? "unset" : kickChannel.getAsMention()));
                        break;

                    case "warn":
                    case "warns":
                        warnChannel = guild.getTextChannelById(warnLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Warn Log \u203C**\n" + (warnChannel == null ? "unset" : warnChannel.getAsMention()));
                        break;

                    case "music":
                        musicChannel = guild.getTextChannelById(musicLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Music Log \uD83C\uDFB5**\n" + (musicChannel == null ? "unset" : musicChannel.getAsMention()));
                        break;

                    case "sdm":
                    case "s-d-m":
                    case "self-deleted-messages":
                        sdmChannel = guild.getTextChannelById(sdmLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Self Deleted Log \uD83D\uDC64**\n" + (sdmChannel == null ? "unset" : sdmChannel.getAsMention()));
                        break;

                    case "odm":
                    case "o-d-m":
                    case "other-deleted-messages":
                        odmChannel = guild.getTextChannelById(odmLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Other Deleted Log \uD83D\uDC65**\n" +
                                (odmChannel == null ? "unset" : odmChannel.getAsMention()));

                        break;
                    case "pm":
                    case "p-m":
                    case "purged-messages":
                        pmChannel = guild.getTextChannelById(pmLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Purge Log \u267B**\n" + (pmChannel == null ? "unset" : pmChannel.getAsMention()));

                        break;
                    case "fm":
                    case "f-m":
                    case "filtered-messages":
                        fmChannel = guild.getTextChannelById(fmLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Filter Log \uD83D\uDEB3**\n" + (fmChannel == null ? "unset" : fmChannel.getAsMention()));
                        break;
                    case "em":
                    case "e-m":
                    case "edited-messages":
                        emChannel = guild.getTextChannelById(emLogChannelCache.getUnchecked(guild.getIdLong()));
                        event.reply("**Filter Log \uD83D\uDEB3**\n" + (emChannel == null ? "unset" : emChannel.getAsMention()));
                        break;
                    default:
                        MessageHelper.sendUsage(this, event);
                        break;
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void setLogChannel(CommandEvent event, ChannelType chosenType, long channelId, LoadingCache<Long, Long> toUse) {
        Guild guild = event.getGuild();
        if (channelId == -1) {
            MessageHelper.sendUsage(this, event);
        } else if (channelId == 0L) {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (toUse.getUnchecked(guild.getIdLong()) != -1 ? "<#" + toUse.getUnchecked(guild.getIdLong()) + ">" : "nothing") + " to nothing by **" + event.getFullAuthorName() + "**");
            TaskScheduler.async(() -> {
                Melijn.mySQL.removeChannel(guild.getIdLong(), chosenType);
                toUse.invalidate(guild.getIdLong());
            });
        } else {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (toUse.getUnchecked(guild.getIdLong()) != -1 ? "<#" + toUse.getUnchecked(guild.getIdLong()) + ">" : "nothing") + " to <#" + channelId + "> by **" + event.getFullAuthorName() + "**");
            TaskScheduler.async(() -> {
                Melijn.mySQL.setChannel(guild.getIdLong(), channelId, chosenType);
                toUse.put(guild.getIdLong(), channelId);
            });
        }
    }
}
