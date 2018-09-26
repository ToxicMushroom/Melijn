package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

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
        this.description = "configures log channels so you can keep track of what's happening in your server";
        this.usage = Melijn.PREFIX + commandName + " <type> [TextChannel | null]";
        this.extra = "Types: all, ban, mute, warn, kick, music, self-deleted-messages, other-deleted-messages, purged-messages, filtered-messages";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
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
                                    banLogChannelCache.invalidate(guild.getIdLong());
                                    muteLogChannelCache.invalidate(guild.getIdLong());
                                    kickLogChannelCache.invalidate(guild.getIdLong());
                                    warnLogChannelCache.invalidate(guild.getIdLong());
                                    musicLogChannelCache.invalidate(guild.getIdLong());
                                    sdmLogChannelCache.invalidate(guild.getIdLong());
                                    odmLogChannelCache.invalidate(guild.getIdLong());
                                    pmLogChannelCache.invalidate(guild.getIdLong());
                                    fmLogChannelCache.invalidate(guild.getIdLong());
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
                                    banLogChannelCache.put(guild.getIdLong(), id);
                                    muteLogChannelCache.put(guild.getIdLong(), id);
                                    kickLogChannelCache.put(guild.getIdLong(), id);
                                    warnLogChannelCache.put(guild.getIdLong(), id);
                                    musicLogChannelCache.put(guild.getIdLong(), id);
                                    sdmLogChannelCache.put(guild.getIdLong(), id);
                                    odmLogChannelCache.put(guild.getIdLong(), id);
                                    pmLogChannelCache.put(guild.getIdLong(), id);
                                    fmLogChannelCache.put(guild.getIdLong(), id);
                                });
                                event.reply("All LogChannels have been changed to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                            }
                            return;
                        case "ban":
                        case "bans":
                            chosenType = ChannelType.BAN_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "mutes":
                        case "mute":
                            chosenType = ChannelType.MUTE_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "kicks":
                        case "kick":
                            chosenType = ChannelType.KICK_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "warns":
                        case "warn":
                            chosenType = ChannelType.WARN_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "songs":
                        case "music":
                            chosenType = ChannelType.MUSIC_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "sdm":
                        case "s-d-m":
                        case "self-deleted-messages":
                            chosenType = ChannelType.SDM_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "odm":
                        case "o-d-m":
                        case "other-deleted-messages":
                            chosenType = ChannelType.ODM_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "pm":
                        case "p-m":
                        case "purged-messages":
                            chosenType = ChannelType.PM_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        case "fm":
                        case "f-m":
                        case "filtered-messages":
                            chosenType = ChannelType.FM_LOG;
                            setLogChannel(event, chosenType, id);
                            break;
                        default:
                            MessageHelper.sendUsage(this, event);
                            break;
                    }
                } else if (args.length == 1 && !args[0].equalsIgnoreCase("")) {
                    switch (args[0].toLowerCase()) {
                        case "all":
                            StringBuilder builder = new StringBuilder();
                            long banChannelId = banLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(banLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : banLogChannelCache.getUnchecked(guild.getIdLong());
                            long muteChannelId = muteLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(muteLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : muteLogChannelCache.getUnchecked(guild.getIdLong());
                            long kickChannelId = kickLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(kickLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : kickLogChannelCache.getUnchecked(guild.getIdLong());
                            long warnChannelId = warnLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(warnLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : warnLogChannelCache.getUnchecked(guild.getIdLong());
                            long sdmChannelId = sdmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(sdmLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : sdmLogChannelCache.getUnchecked(guild.getIdLong());
                            long odmChannelId = odmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(odmLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : odmLogChannelCache.getUnchecked(guild.getIdLong());
                            long pmChannelId = pmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(pmLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : pmLogChannelCache.getUnchecked(guild.getIdLong());
                            long fmChannelId = fmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(fmLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : fmLogChannelCache.getUnchecked(guild.getIdLong());
                            long musicChannelId = musicLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(musicLogChannelCache.getUnchecked(guild.getIdLong())) == null ? -1L : musicLogChannelCache.getUnchecked(guild.getIdLong());

                            builder.append("**Log Channels :clipboard:**\n")
                                    .append("  Bans: ").append(banChannelId == -1L ? "unset" : "<#" + banChannelId + ">").append("\n")
                                    .append("  Mutes: ").append(muteChannelId == -1L ? "unset" : "<#" + muteChannelId + ">").append("\n")
                                    .append("  Kicks: ").append(kickChannelId == -1L ? "unset" : "<#" + kickChannelId + ">").append("\n")
                                    .append("  Warns: ").append(warnChannelId == -1L ? "unset" : "<#" + warnChannelId + ">").append("\n")
                                    .append("  SelfDeleteMessages: ").append(sdmChannelId == -1L ? "unset" : "<#" + sdmChannelId + ">").append("\n")
                                    .append("  OtherDeleteMessages: ").append(odmChannelId == -1L ? "unset" : "<#" + odmChannelId + ">").append("\n")
                                    .append("  PurgedMessages: ").append(pmChannelId == -1L ? "unset" : "<#" + pmChannelId + ">").append("\n")
                                    .append("  FilteredMessages: ").append(fmChannelId == -1L ? "unset" : "<#" + fmChannelId + ">").append("\n")
                                    .append("  Music: ").append(musicChannelId == -1L ? "unset" : "<#" + musicChannelId + ">").append("\n");
                            event.reply(builder.toString());
                            break;
                        case "ban":
                        case "bans":
                            event.reply("**Ban Log \uD83D\uDD28**\n" +
                                    ((banLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(banLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + banLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));
                            break;
                        case "mute":
                        case "mutes":
                            event.reply("**Mute Log \uD83E\uDD10**\n" +
                                    ((muteLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(muteLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + muteLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "kick":
                        case "kicks":
                            event.reply("**Kick Log \uD83E\uDD1C\uD83D\uDCA2**\n" +
                                    ((kickLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(kickLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + kickLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "warn":
                        case "warns":
                            event.reply("**Warn Log \u203C**\n" +
                                    ((warnLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(warnLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + warnLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "music":
                            event.reply("**Music Log \uD83C\uDFB5**\n" +
                                    ((musicLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(musicLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + musicLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "sdm":
                        case "s-d-m":
                        case "self-deleted-messages":
                            event.reply("**Self Deleted Log \uD83D\uDC64**\n" +
                                    ((sdmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(sdmLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + sdmLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "odm":
                        case "o-d-m":
                        case "other-deleted-messages":
                            event.reply("**Other Deleted Log \uD83D\uDC65**\n" +
                                    ((odmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(odmLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + odmLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "pm":
                        case "p-m":
                        case "purged-messages":
                            event.reply("**Purge Log \u267B**\n" +
                                    ((pmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(pmLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + pmLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));

                            break;
                        case "fm":
                        case "f-m":
                        case "filtered-messages":
                            event.reply("**Filter Log \uD83D\uDEB3**\n" +
                                    ((fmLogChannelCache.getUnchecked(guild.getIdLong()) == -1 || guild.getTextChannelById(fmLogChannelCache.getUnchecked(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + fmLogChannelCache.getUnchecked(guild.getIdLong()) + ">"));
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
        } else {
            event.reply(Helpers.guildOnly);
        }
    }

    private void setLogChannel(CommandEvent event, ChannelType chosenType, long channelId) {
        Guild guild = event.getGuild();
        if (channelId == -1) {
            MessageHelper.sendUsage(this, event);
        } else if (channelId == 0L) {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (muteLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ? "<#" + muteLogChannelCache.getUnchecked(guild.getIdLong()) + ">" : "nothing") + " to nothing by **" + event.getFullAuthorName() + "**");
            TaskScheduler.async(() -> {
                Melijn.mySQL.removeChannel(guild.getIdLong(), chosenType);
                muteLogChannelCache.invalidate(guild.getIdLong());
            });
        } else {
            event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (muteLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ? "<#" + muteLogChannelCache.getUnchecked(guild.getIdLong()) + ">" : "nothing") + " to <#" + channelId + "> by **" + event.getFullAuthorName() + "**");
            TaskScheduler.async(() -> {
                Melijn.mySQL.setChannel(guild.getIdLong(), channelId, chosenType);
                muteLogChannelCache.put(guild.getIdLong(), channelId);
            });
        }
    }
}
