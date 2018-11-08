package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.concurrent.BlockingQueue;

import static me.melijn.jda.Melijn.PREFIX;

public class SkipCommand extends Command {

    private MusicManager manager = MusicManager.getManagerInstance();

    public SkipCommand() {
        this.commandName = "skip";
        this.description = "Skip to a song in the queue";
        this.usage = PREFIX + commandName + " [1-50]";
        this.category = Category.MUSIC;
        this.aliases = new String[]{"s"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = manager.getPlayer(event.getGuild());
            AudioTrack shippableTrack = player.getAudioPlayer().getPlayingTrack();
            if (shippableTrack == null) {
                event.reply("There are no songs playing at the moment");
                return;
            }
            String[] args = event.getArgs().split("\\s+");
            BlockingQueue<AudioTrack> audioTracks = player.getListener().getTracks();
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
                if (i != c) {
                    nextSong = track;
                    player.skipTrack();
                    c++;
                }
            }
            String songOrSongs = i == 1 ? "song" : "songs";
            EmbedBuilder eb = new Embedder(event.getGuild());
            eb.setTitle("Skipped " + i + " " + songOrSongs);
            if (nextSong != null)
                eb.setDescription("Previous song: **[" + shippableTrack.getInfo().title + "](" + shippableTrack.getInfo().uri + ")**\n" + "Now playing: **[" + nextSong.getInfo().title + "](" + nextSong.getInfo().uri + ")** " + Helpers.getDurationBreakdown(nextSong.getInfo().length));
            else {
                player.skipTrack();
                eb.setDescription("Previous song: **[" + shippableTrack.getInfo().title + "](" + shippableTrack.getInfo().uri + ")**\n" + "No next song to play");
            }
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.reply(eb.build());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
