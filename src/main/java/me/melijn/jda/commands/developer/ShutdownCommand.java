package me.melijn.jda.commands.developer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import static me.melijn.jda.Melijn.PREFIX;

public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        this.commandName = "shutdown";
        this.description = "shut's the bot nicely down";
        this.usage = PREFIX + commandName;
        this.category = Category.DEVELOPER;
        this.id = 101;
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            File file = new File("Melijn.mp3");
            //save players before shutdown
            TLongObjectMap<MusicPlayer> players = AudioLoader.getManagerInstance().getPlayers();
            players.forEachValue((player) -> {
                Guild guild = event.getJDA().asBot().getShardManager().getGuildById(player.getGuildId());
                if (guild == null || !guild.getSelfMember().getVoiceState().inVoiceChannel()) return true;
                TaskScheduler.async(() -> Lava.lava.closeConnection(player.getGuildId()), 9000);
                boolean paused = player.isPaused();
                Queue<AudioTrack> queue = new LinkedList<>();
                if (player.getAudioPlayer().getPlayingTrack() != null) queue.offer(player.getAudioPlayer().getPlayingTrack());
                player.getTrackManager().getTracks().forEach(queue::offer);
                Melijn.mySQL.addQueue(guild.getIdLong(), guild.getSelfMember().getVoiceState().getChannel().getIdLong(), paused, queue);
                player.getTrackManager().getTracks().clear();
                player.getAudioPlayer().stopTrack();
                AudioLoader.getManagerInstance().loadSimpleTrack(player, file.getAbsolutePath());
                return true;
            });
            event.reply("Shutting down in 9 seconds");
            TaskScheduler.async(() -> event.getJDA().shutdown(), 10_000);
        } catch (Exception e) {
            e.printStackTrace();
            event.reply("something went wrong :/");
        }
    }
}
