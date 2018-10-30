package me.melijn.jda.commands.developer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.TaskScheduler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static me.melijn.jda.Melijn.PREFIX;

public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        this.commandName = "shutdown";
        this.description = "shut's the bot nicely down";
        this.usage = PREFIX + commandName;
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        URL resource = getClass().getClassLoader().getResource("Melijn.mp3");
        if (resource == null) {
            event.reply("mp3 not found :/");
            return;
        }
        try {
            File tempFile = File.createTempFile(FilenameUtils.getBaseName(resource.getFile()), FilenameUtils.getExtension(resource.getFile()));
            IOUtils.copy(resource.openStream(), FileUtils.openOutputStream(tempFile));

            //save players before shutdown
            TLongObjectMap<MusicPlayer> players = MusicManager.getManagerInstance().getPlayers();
            players.forEachValue((player) -> {
                TaskScheduler.async(() -> Helpers.scheduleClose(player.getGuild().getAudioManager()), 9000);
                boolean paused = player.getPaused();
                BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();
                if (player.getAudioPlayer().getPlayingTrack() != null)
                    queue.offer(player.getAudioPlayer().getPlayingTrack());
                player.getListener().getTracks().forEach(queue::offer);
                Melijn.mySQL.addQueue(player.getGuild().getIdLong(), player.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong(), paused, queue);
                player.getAudioPlayer().stopTrack();
                player.getListener().getTracks().clear();
                MusicManager.getManagerInstance().loadSimpleTrack(player.getGuild(), tempFile.getPath());
                return true;
            });
            event.reply("Shutting down in 9 seconds");
            TaskScheduler.async(() -> event.getJDA().shutdown(), 9000);
        } catch (IOException e) {
            e.printStackTrace();
            event.reply("mp3 not found :/");
        }
    }
}
