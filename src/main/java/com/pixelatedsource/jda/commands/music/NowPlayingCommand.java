package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class NowPlayingCommand extends Command {

    public NowPlayingCommand() {
        this.commandName = "np";
        this.description = "Show you the song that the bot is playing";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"nowplaying", "playing", "nplaying", "nowp"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                AudioTrack track = MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
                if (track == null) event.reply("There are no songs playing at the moment.");
                else event.reply(new EmbedBuilder().setTitle("Now playing").setColor(Helpers.EmbedColor).setDescription(track.getInfo().title + " `" + Helpers.getDurationBreakdown(track.getPosition()) + " / " + Helpers.getDurationBreakdown(track.getInfo().length) + "`").setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon()).build());
            }
        }
    }
}
