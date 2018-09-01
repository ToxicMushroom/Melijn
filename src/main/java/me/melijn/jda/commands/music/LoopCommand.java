package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static me.melijn.jda.Melijn.PREFIX;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Change the looping state or view the looping state of the playing song";
        this.usage = PREFIX + this.commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeat", "loopsong"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    public static HashMap<Long, Boolean> looped = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        executorLoops(this, event, looped);
    }

    static void executorLoops(Command cmd, CommandEvent event, HashMap<Long, Boolean> looped) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), cmd.getCommandName(), 0)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (MusicManager.getManagerInstance().getPlayer(guild).getListener().getTrackSize() > 0 || MusicManager.getManagerInstance().getPlayer(guild).getAudioPlayer().getPlayingTrack() != null) {
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (looped.containsKey(guild.getIdLong())) {
                        if (looped.get(guild.getIdLong())) {
                            looped.replace(guild.getIdLong(), false);
                            event.reply("Looping has been **disabled**");
                        } else {
                            looped.replace(guild.getIdLong(), true);
                            event.reply("Looping has been **enabled**");
                        }
                    } else {
                        looped.put(guild.getIdLong(), true);
                        event.reply("Looping has been **enabled**");
                    }
                } else {
                    switch (args[0]) {
                        case "on":
                        case "yes":
                        case "true":
                            if (looped.replace(guild.getIdLong(), true) == null)
                                looped.put(guild.getIdLong(), true);
                            event.reply("Looping has been **enabled**");
                            break;
                        case "off":
                        case "no":
                        case "false":
                            if (looped.replace(guild.getIdLong(), false) == null)
                                looped.put(guild.getIdLong(), false);
                            event.reply("Looping has been **disabled**");
                            break;
                        case "info":
                            String ts = looped.getOrDefault(guild.getIdLong(), false) ? "enabled" : "disabled";
                            event.reply("Looping is currently **" + ts + "**");
                            break;
                        default:
                            MessageHelper.sendUsage(cmd, event);
                            break;
                    }
                }
            } else {
                event.reply("There is no music playing");
            }
        } else {
            event.reply("You need the permission `" + cmd.getCommandName() + "` to execute this command.");
        }
    }
}
