package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static me.melijn.jda.Melijn.PREFIX;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        this.commandName = "shuffle";
        this.description = "shuffles the order of the tracks in the queue";
        this.usage = PREFIX + commandName;
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.aliases = new String[]{"randomize"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            Object[] array = player.getListener().tracks.toArray();
            List<Object> tracks = Arrays.asList(array);
            Collections.shuffle(tracks);
            BlockingQueue<AudioTrack> tracksToAdd = new LinkedBlockingQueue<>();
            tracks.forEach(s -> tracksToAdd.add((AudioTrack) s));
            player.getListener().tracks.clear();
            player.getListener().tracks.addAll(tracksToAdd);
            event.reply("The queue has been **shuffled** by **" + event.getFullAuthorName() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
