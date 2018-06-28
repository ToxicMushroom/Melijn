package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class RewindCommand extends Command {

    public RewindCommand() {
        this.commandName = "rewind";
        this.description = "rewind inside the song (20s/2m50s - 5s -> 15s/2m50s)";
        this.usage = PREFIX + commandName + " [xx:xx:xx]";
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            if (args.length == 1 && args[0].equalsIgnoreCase("")) args = new String[0];
            AudioTrack player = MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
            if (player != null) {
                long millis = Helpers.parseTimeFromArgs(args);
                if (millis != -1) {
                    player.setPosition(player.getPosition() - millis);
                    event.reply("The position of the song has been changed to **" + Helpers.getDurationBreakdown(player.getPosition()) + "/" + Helpers.getDurationBreakdown(player.getDuration()) + "** by **" + event.getFullAuthorName() + "**");
                } else MessageHelper.sendUsage(this, event);
            } else {
                event.reply("There are no songs playing at the moment.");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
