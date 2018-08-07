package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;

public class PauseCommand extends Command {

    public PauseCommand() {
        this.commandName = "pause";
        this.description = "pause the queue without stopping or deleting songs";
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.usage = PREFIX + commandName + " [on/enable/true | off/disable/false]";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            String[] args = event.getArgs().split("\\s+");
            if (player.getAudioPlayer().getPlayingTrack() != null || player.getListener().getTrackSize() > 0) {
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (player.getAudioPlayer().isPaused()) {
                        player.getAudioPlayer().setPaused(false);
                        event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                    } else {
                        player.getAudioPlayer().setPaused(true);
                        event.reply("Paused by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                    }
                } else if (args.length == 1) {
                    switch (args[0]) {
                        case "on":
                        case "enable":
                        case "true":
                            player.getAudioPlayer().setPaused(true);
                            event.reply("Paused by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                            break;
                        case "off":
                        case "disable":
                        case "false":
                            player.getAudioPlayer().setPaused(false);
                            event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                            break;
                        case "info":
                            String s = player.getPaused() ? "paused" : "playing";
                            event.reply("The music is currently **" + s + "**.");
                            break;
                        default:
                            MessageHelper.sendUsage(this, event);
                            break;
                    }
                }
            } else {
                event.reply("There are no songs playing at the moment");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
