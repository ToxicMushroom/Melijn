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
        this.aliases = new String[]{"randomize"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
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
                        event.reply("You have to be in the same voice channel as me to shuffle");
                    }
                } else {
                    event.reply("I'm not in a voiceChannel");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
