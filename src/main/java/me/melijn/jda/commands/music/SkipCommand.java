package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.Queue;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.audio.Lava.lava;

public class SkipCommand extends Command {

    private AudioLoader manager = AudioLoader.getManagerInstance();

    public SkipCommand() {
        this.commandName = "skip";
        this.description = "Skips to a song in the queue";
        this.usage = PREFIX + commandName + " [1-50]";
        this.category = Category.MUSIC;
        this.aliases = new String[]{"s"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.id = 61;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = manager.getPlayer(event.getGuild());
            AudioTrack skipableTrack = player.getAudioPlayer().getPlayingTrack();
            if (skipableTrack == null) {
                event.reply("There are no tracks playing");
                return;
            }
            String[] args = event.getArgs().split("\\s+");
            Queue<AudioTrack> audioTracks = player.getTrackManager().getTracks();
            int i = 1;
            if (args.length > 0 && !args[0].isBlank()) {
                if (args[0].matches("\\d+") && args[0].length() < 4) {
                    i = Integer.parseInt(args[0]);
                    if (i >= 50 || i < 1) {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                    return;
                }
            }
            AudioTrack nextSong = null;
            int c = 0;
            for (AudioTrack track : audioTracks) {
                if (i == ++c) {
                    nextSong = track;
                    player.skipTrack();
                    break;
                }
                player.getTrackManager().tracks.poll();
            }
            EmbedBuilder eb = new Embedder(event.getGuild());
            eb.setTitle("Skipped " + i + " " + (i == 1 ? "song" : "songs"));
            if (nextSong != null)
                eb.setDescription("Previous song: **[" + skipableTrack.getInfo().title + "](" + skipableTrack.getInfo().uri + ")**\n" + "Now playing: **[" + nextSong.getInfo().title + "](" + nextSong.getInfo().uri + ")** " + Helpers.getDurationBreakdown(nextSong.getInfo().length));
            else {
                player.stopTrack();
                player.getTrackManager().clear();
                lava.closeConnection(event.getGuild().getIdLong());
                eb.setDescription("Previous song: **[" + skipableTrack.getInfo().title + "](" + skipableTrack.getInfo().uri + ")**\n" + "No next song to play");
            }
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.reply(eb.build());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
