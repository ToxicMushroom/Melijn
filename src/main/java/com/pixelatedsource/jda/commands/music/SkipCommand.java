package com.pixelatedsource.jda.commands.music;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.concurrent.BlockingQueue;

import static net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS;

public class SkipCommand extends Command {

    public SkipCommand() {
        this.name = "skip";
        this.help = "Usage: " + PixelSniper.PREFIX + this.name + " [1-50]";
        this.guildOnly = true;
        this.botPermissions = new Permission[] {MESSAGE_EMBED_LINKS};
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            MusicPlayer player = manager.getPlayer(event.getGuild());
            AudioTrack tracknp = player.getAudioPlayer().getPlayingTrack();
            if (tracknp == null) {
                event.reply("Their are no songs playing at the moment.");
                return;
            }
            String[] args = event.getArgs().split("\\s+");
            BlockingQueue<AudioTrack> audioTracks = player.getListener().getTracks();
            int i = 1;
            if (args.length > 0) {
                if (!args[0].equalsIgnoreCase("")) {
                    try {
                        i = Integer.parseInt(args[0]);
                        if (i >= 50 || i < 1) {
                            event.reply("... dude how hard is it to pick a number from 1 to 50");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        e.addSuppressed(e);
                        event.reply("... dude how hard is it to pick a number from 1 to 50");
                    }
                }
            }
            AudioTrack nextSong = null;
            int c = 0;
            for (AudioTrack track : audioTracks) {
                if (i != c) {
                    nextSong = track;
                    player.skipTrack();
                    c++;
                }
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Skipped");
            eb.setColor(Helpers.EmbedColor);
            String songOrSongs = i == 1 ? "song" : "songs";
            if (nextSong != null)
                eb.setDescription("Skipped " + i + " " + songOrSongs + ": `" + tracknp.getInfo().title + "`\n" + "Now playing the next song: `" + nextSong.getInfo().title + "` " +
                        Helpers.getDurationBreakdown(nextSong.getInfo().length));
            else {
                player.skipTrack();
                eb.setDescription("Skipped " + i + " " + songOrSongs + ": `" + tracknp.getInfo().title + "`\n" + "No next song to play :/.");
            }
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.reply(eb.build());
        }
    }
}
