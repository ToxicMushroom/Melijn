package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        this.commandName = "shuffle";
        this.description = "shuffles the order of the tracks in the queue";
        this.usage = PREFIX + commandName;
        this.aliases = new String[] {"randomize"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
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
            }
        }
    }
}
