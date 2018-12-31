package me.melijn.jda.commands.music;

import lavalink.client.player.LavalinkPlayer;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;

public class RewindCommand extends Command {

    public RewindCommand() {
        this.commandName = "rewind";
        this.description = "Rewinds inside the track";
        this.usage = PREFIX + commandName + " [hh:mm:ss]";
        this.extra = "e.g. >rewind 11 -> -11s | >rewind 1:01 -> -61s | >rewind 1:02:01 -> -3721s";
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 14;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            LavalinkPlayer player = AudioLoader.getManagerInstance().getPlayer(event.getGuild()).getAudioPlayer();
            if (player.getPlayingTrack() != null) {
                long millis = Helpers.parseTimeFromArgs(args);
                if (millis == -1) {
                    MessageHelper.sendUsage(this, event);
                    return;
                }
                millis = player.getTrackPosition() - millis;
                player.seekTo(millis);
                event.reply("The position of the song has been changed to **" +
                        Helpers.getDurationBreakdown(Math.max(millis, 0)) + "/" +
                        Helpers.getDurationBreakdown(player.getPlayingTrack().getDuration()) + "** by **" + event.getFullAuthorName() + "**");

            } else {
                event.reply("There are no songs playing at the moment.");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
