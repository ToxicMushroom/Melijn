package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.concurrent.BlockingQueue;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SkipCommand extends Command {

    public SkipCommand() {
        this.commandName = "skip";
        this.description = "Skip to a song in the queue";
        this.usage = PREFIX + this.commandName + " [1-50]";
        this.category = Category.MUSIC;
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
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
                                if (event.getGuild() != null) {
                                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                                } else {
                                    event.reply(usage);
                                }
                                return;
                            }
                        } catch (NumberFormatException e) {
                            e.addSuppressed(e);
                            if (event.getGuild() != null) {
                                event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                            } else {
                                event.reply(usage);
                            }
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
                if (nextSong != null) eb.setDescription("Skipped " + i + " " + songOrSongs + ": `" + tracknp.getInfo().title + "`\n" + "Now playing the next song: `" + nextSong.getInfo().title + "` " + Helpers.getDurationBreakdown(nextSong.getInfo().length));
                else {
                    player.skipTrack();
                    eb.setDescription("Skipped " + i + " " + songOrSongs + ": `" + tracknp.getInfo().title + "`\n" + "No next song to play :/.");
                }
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
