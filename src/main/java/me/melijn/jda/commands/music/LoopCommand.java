package me.melijn.jda.commands.music;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class LoopCommand extends Command {

    public static TLongList looped = new TLongArrayList();

    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Changes the looping state of the playing track";
        this.usage = PREFIX + commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeat", "loopsong"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 68;
    }

    @Override
    protected void execute(CommandEvent event) {
        executorLoops(this, event, looped);
    }

    static void executorLoops(Command cmd, CommandEvent event, TLongList looped) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), cmd.getCommandName(), 0)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            MusicPlayer musicPlayer = AudioLoader.getManagerInstance().getPlayer(guild);
            if (musicPlayer.getTrackManager().getTrackSize() > 0 || musicPlayer.getAudioPlayer().getPlayingTrack() != null) {
                if (args.length == 0 || args[0].isBlank()) {
                    if (looped.contains(guild.getIdLong())) {
                        looped.remove(guild.getIdLong());
                        event.reply("Looping has been **disabled**");
                    } else {
                        looped.add(guild.getIdLong());
                        event.reply("Looping has been **enabled**");
                    }
                } else {
                    switch (args[0]) {
                        case "on":
                        case "yes":
                        case "true":
                            if (!looped.contains(guild.getIdLong())) looped.add(guild.getIdLong());
                            event.reply("Looping has been **enabled**");
                            break;
                        case "off":
                        case "no":
                        case "false":
                            looped.remove(guild.getIdLong());
                            event.reply("Looping has been **disabled**");
                            break;
                        case "info":
                            String ts = looped.contains(guild.getIdLong()) ? "enabled" : "disabled";
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
