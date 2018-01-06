package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;

public class NowPlayingCommand extends Command {

    public NowPlayingCommand() {
        this.name = "np";
        this.aliases = new String[] {"nowplaying", "playing", "nplaying", "nowp"};
        this.help = "Show you the song that the bot is playing";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            AudioTrack track = MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
            if (track == null)
                event.reply("There are no songs playing at the moment.");
            else
                event.reply(new EmbedBuilder()
                        .setTitle("Now playing")
                        .setColor(Helpers.EmbedColor)
                        .setDescription(track.getInfo().title + " `" + Helpers.getDurationBreakdown(track.getPosition()) +" / " +  Helpers.getDurationBreakdown(track.getInfo().length) + "`")
                        .setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon())
                        .build());
        }
    }
}
