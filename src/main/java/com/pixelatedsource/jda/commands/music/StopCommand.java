package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class StopCommand extends Command {

    public StopCommand() {
        this.commandName = "stop";
        this.description = "Stops the current playing song and pauses the queue and disconnects from the connected voice channel";
        this.usage = PREFIX + this.commandName;
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                LoopCommand.looped.put(event.getGuild(), false);
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                player.stopTrack();
                event.reply("Stopped by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
