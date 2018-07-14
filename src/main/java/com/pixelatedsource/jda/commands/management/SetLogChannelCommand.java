package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetLogChannelCommand extends Command {

    public SetLogChannelCommand() {
        this.commandName = "setLogChannel";
        this.description = "configures log channels so you can keep track of what's happening in your server";
        this.usage = PREFIX + commandName + " <type> [TextChannel | null]";
        this.extra = "Types: all, ban, mute, warn, kick, music, self-deleted-messages,other-deleted-messages, purged-messages, filtered-messages";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> banLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.BAN_LOG);
    public static HashMap<Long, Long> muteLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.MUTE_LOG);
    public static HashMap<Long, Long> kickLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.KICK_LOG);
    public static HashMap<Long, Long> warnLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.WARN_LOG);
    public static HashMap<Long, Long> musicLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.MUSIC_LOG);
    public static HashMap<Long, Long> sdmLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.SDM_LOG);
    public static HashMap<Long, Long> odmLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.ODM_LOG);
    public static HashMap<Long, Long> pmLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.PM_LOG);
    public static HashMap<Long, Long> fmLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.FM_LOG);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 1) {
                    HashMap<Long, Long> chosenMap = new HashMap<>();
                    ChannelType chosenType = null;
                    switch (args[0].toLowerCase()) {
                        case "all":
                            long id = Helpers.getTextChannelByArgsN(event, args[0]);
                            if (id == -1) {
                                MessageHelper.sendUsage(this, event);
                            } else if (id == 0L) {
                                banLogChannelMap.remove(guild.getIdLong());
                                muteLogChannelMap.remove(guild.getIdLong());
                                kickLogChannelMap.remove(guild.getIdLong());
                                warnLogChannelMap.remove(guild.getIdLong());
                                musicLogChannelMap.remove(guild.getIdLong());
                                sdmLogChannelMap.remove(guild.getIdLong());
                                odmLogChannelMap.remove(guild.getIdLong());
                                pmLogChannelMap.remove(guild.getIdLong());
                                fmLogChannelMap.remove(guild.getIdLong());
                                new Thread(() -> {
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.BAN_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.KICK_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.WARN_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                                    PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                                }).start();
                                event.reply("All LogChannels have been changed to nothing by **" + event.getFullAuthorName() + "**");
                            } else {
                                if (banLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    banLogChannelMap.put(guild.getIdLong(), id);
                                if (muteLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    muteLogChannelMap.put(guild.getIdLong(), id);
                                if (kickLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    kickLogChannelMap.put(guild.getIdLong(), id);
                                if (warnLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    warnLogChannelMap.put(guild.getIdLong(), id);
                                if (musicLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    musicLogChannelMap.put(guild.getIdLong(), id);
                                if (sdmLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    sdmLogChannelMap.put(guild.getIdLong(), id);
                                if (odmLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    odmLogChannelMap.put(guild.getIdLong(), id);
                                if (pmLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    pmLogChannelMap.put(guild.getIdLong(), id);
                                if (fmLogChannelMap.replace(guild.getIdLong(), id) == null)
                                    fmLogChannelMap.put(guild.getIdLong(), id);
                                chosenMap.put(guild.getIdLong(), id);
                                new Thread(() -> {
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.BAN_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.KICK_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WARN_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.SDM_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.ODM_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.PM_LOG);
                                    PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.FM_LOG);
                                }).start();
                                event.reply("All LogChannels have been changed to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                            }
                            return;
                        case "ban":
                            chosenMap = banLogChannelMap;
                            chosenType = ChannelType.BAN_LOG;
                            break;
                        case "mute":
                            chosenMap = muteLogChannelMap;
                            chosenType = ChannelType.MUTE_LOG;
                            break;
                        case "kick":
                            chosenMap = kickLogChannelMap;
                            chosenType = ChannelType.KICK_LOG;
                            break;
                        case "warn":
                            chosenMap = warnLogChannelMap;
                            chosenType = ChannelType.WARN_LOG;
                            break;
                        case "music":
                            chosenMap = musicLogChannelMap;
                            chosenType = ChannelType.MUSIC_LOG;
                            break;
                        case "sdm":
                        case "s-d-m":
                        case "sther-deleted-messages":
                            chosenMap = sdmLogChannelMap;
                            chosenType = ChannelType.SDM_LOG;
                            break;
                        case "odm":
                        case "o-d-m":
                        case "other-deleted-messages":
                            chosenMap = odmLogChannelMap;
                            chosenType = ChannelType.ODM_LOG;
                            break;
                        case "pm":
                        case "p-m":
                        case "purged-messages":
                            chosenMap = pmLogChannelMap;
                            chosenType = ChannelType.PM_LOG;
                            break;
                        case "fm":
                        case "f-m":
                        case "filtered-messages":
                            chosenMap = fmLogChannelMap;
                            chosenType = ChannelType.FM_LOG;
                            break;
                    }

                    if (chosenType != null) {
                        long id = Helpers.getTextChannelByArgsN(event, args[0]);
                        if (id == -1) {
                            MessageHelper.sendUsage(this, event);
                        } else if (id == 0L) {
                            ChannelType finalChosenType = chosenType;
                            event.reply("LogChannel has been changed from " + (chosenMap.containsKey(guild.getIdLong()) ? "<#" + chosenMap.get(guild.getIdLong()) + ">" : "nothing") + " to nothing by **" + event.getFullAuthorName() + "**");
                            chosenMap.remove(guild.getIdLong());
                            new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), finalChosenType)).start();
                        } else {
                            ChannelType finalChosenType1 = chosenType;
                            event.reply("LogChannel has been changed from " + (chosenMap.containsKey(guild.getIdLong()) ? "<#" + chosenMap.get(guild.getIdLong()) + ">" : "nothing") + " to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                            if (chosenMap.replace(guild.getIdLong(), id) == null) chosenMap.put(guild.getIdLong(), id);
                            new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), id, finalChosenType1)).start();
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
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
}
