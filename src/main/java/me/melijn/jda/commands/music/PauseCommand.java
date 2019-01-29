package me.melijn.jda.commands.music;

import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class PauseCommand extends Command {

    public PauseCommand() {
        this.commandName = "pause";
        this.description = "Pauses the queue";
        this.usage = PREFIX + commandName + " [on/enable/true | off/disable/false]";
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.id = 44;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            MusicPlayer player = event.getClient().getMelijn().getLava().getAudioLoader().getPlayer(event.getGuild());
            String[] args = event.getArgs().split("\\s+");
            if (player.getAudioPlayer().getPlayingTrack() != null || player.getTrackManager().getTrackSize() > 0) {
                if (args.length == 0 || args[0].isEmpty()) {
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
                            String s = player.isPaused() ? "paused" : "playing";
                            event.reply("The music is currently **" + s + "**.");
                            break;
                        default:
                            event.sendUsage(this, event);
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
