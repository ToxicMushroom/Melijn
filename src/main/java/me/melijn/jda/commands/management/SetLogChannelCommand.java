package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class SetLogChannelCommand extends Command {

    public SetLogChannelCommand() {
        this.commandName = "setLogChannel";
        this.description = "configures log channels so you can keep track of what's happening in your server";
        this.usage = Melijn.PREFIX + commandName + " <type> [TextChannel | null]";
        this.extra = "Types: all, ban, mute, warn, kick, music, self-deleted-messages, other-deleted-messages, purged-messages, filtered-messages";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> banLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.BAN_LOG);
    public static HashMap<Long, Long> muteLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.MUTE_LOG);
    public static HashMap<Long, Long> kickLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.KICK_LOG);
    public static HashMap<Long, Long> warnLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.WARN_LOG);
    public static HashMap<Long, Long> musicLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.MUSIC_LOG);
    public static HashMap<Long, Long> sdmLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.SDM_LOG);
    public static HashMap<Long, Long> odmLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.ODM_LOG);
    public static HashMap<Long, Long> pmLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.PM_LOG);
    public static HashMap<Long, Long> fmLogChannelMap = Melijn.mySQL.getChannelMap(ChannelType.FM_LOG);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 1) {
                    HashMap<Long, Long> chosenMap = new HashMap<>();
                    ChannelType chosenType;
                    switch (args[0].toLowerCase()) {
                        case "all":
                            long id = Helpers.getTextChannelByArgsN(event, args[1]);
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
                                Melijn.MAIN_THREAD.submit(() -> {
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.BAN_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.KICK_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.WARN_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                                });
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
                                Melijn.MAIN_THREAD.submit(() -> {
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.BAN_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.KICK_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WARN_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.MUSIC_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.SDM_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.ODM_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.PM_LOG);
                                    Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.FM_LOG);
                                });
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
                        case "self-deleted-messages":
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
                        default:
                            MessageHelper.sendUsage(this, event);
                            return;
                    }

                    long id = Helpers.getTextChannelByArgsN(event, args[1]);
                    if (id == -1) {
                        MessageHelper.sendUsage(this, event);
                    } else if (id == 0L) {
                        event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (chosenMap.containsKey(guild.getIdLong()) ? "<#" + chosenMap.get(guild.getIdLong()) + ">" : "nothing") + " to nothing by **" + event.getFullAuthorName() + "**");
                        chosenMap.remove(guild.getIdLong());
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.removeChannel(guild.getIdLong(), chosenType));
                    } else {
                        event.reply(chosenType.toString() + "_CHANNEL has been changed from " + (chosenMap.containsKey(guild.getIdLong()) ? "<#" + chosenMap.get(guild.getIdLong()) + ">" : "nothing") + " to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                        if (chosenMap.replace(guild.getIdLong(), id) == null) chosenMap.put(guild.getIdLong(), id);
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setChannel(guild.getIdLong(), id, chosenType));
                    }
                } else if (args.length == 1 && !args[0].equalsIgnoreCase("")) {
                    switch (args[0].toLowerCase()) {
                        case "all":
                            StringBuilder builder = new StringBuilder();
                            long banChannelId = !banLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(banLogChannelMap.get(guild.getIdLong())) == null ? -1L : banLogChannelMap.get(guild.getIdLong());
                            long muteChannelId = !muteLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(muteLogChannelMap.get(guild.getIdLong())) == null ? -1L : muteLogChannelMap.get(guild.getIdLong());
                            long kickChannelId = !kickLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(kickLogChannelMap.get(guild.getIdLong())) == null ? -1L : kickLogChannelMap.get(guild.getIdLong());
                            long warnChannelId = !warnLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(warnLogChannelMap.get(guild.getIdLong())) == null ? -1L : warnLogChannelMap.get(guild.getIdLong());
                            long sdmChannelId = !sdmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(sdmLogChannelMap.get(guild.getIdLong())) == null ? -1L : sdmLogChannelMap.get(guild.getIdLong());
                            long odmChannelId = !odmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(odmLogChannelMap.get(guild.getIdLong())) == null ? -1L : odmLogChannelMap.get(guild.getIdLong());
                            long pmChannelId = !pmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(pmLogChannelMap.get(guild.getIdLong())) == null ? -1L : pmLogChannelMap.get(guild.getIdLong());
                            long fmChannelId = !fmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(fmLogChannelMap.get(guild.getIdLong())) == null ? -1L : fmLogChannelMap.get(guild.getIdLong());
                            long musicChannelId = !musicLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(musicLogChannelMap.get(guild.getIdLong())) == null ? -1L : musicLogChannelMap.get(guild.getIdLong());

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
                            event.reply("**Ban Log :hammer:**\n- " +
                                    ((!banLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(banLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + banLogChannelMap.get(guild.getIdLong()) + ">"));
                            break;
                        case "mute":
                            event.reply("**Mute Log :zipper_mouth:**\n- " +
                                    ((!muteLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(muteLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + muteLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "kick":
                            event.reply("**Kick Log :right_facing_fist::anger:**\n- " +
                                    ((!kickLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(kickLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + kickLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "warn":
                            event.reply("**Warn Log :bangbang:**\n- " +
                                    ((!warnLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(warnLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + warnLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "music":
                            event.reply("**Music Log :musical_note:**\n- " +
                                    ((!musicLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(musicLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + musicLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "sdm":
                        case "s-d-m":
                        case "self-deleted-messages":
                            event.reply("**Self Deleted Log :bust_in_silhouette:**\n- " +
                                    ((!sdmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(sdmLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + sdmLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "odm":
                        case "o-d-m":
                        case "other-deleted-messages":
                            event.reply("**Other Deleted Log :busts_in_silhouette:**\n- " +
                                    ((!odmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(odmLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + odmLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "pm":
                        case "p-m":
                        case "purged-messages":
                            event.reply("**Purge Log :recycle:**\n- " +
                                    ((!pmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(pmLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + pmLogChannelMap.get(guild.getIdLong()) + ">"));

                            break;
                        case "fm":
                        case "f-m":
                        case "filtered-messages":
                            event.reply("**Filter Log :no_bicycles:**\n- " +
                                    ((!fmLogChannelMap.containsKey(guild.getIdLong()) || guild.getTextChannelById(fmLogChannelMap.get(guild.getIdLong())) == null) ?
                                            "unset" :
                                            "<#" + fmLogChannelMap.get(guild.getIdLong()) + ">"));
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
}
