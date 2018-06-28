package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class StopCommand extends Command {

    public StopCommand() {
        this.commandName = "stop";
        this.description = "Stops the current playing song and pauses the queue and disconnects from the connected voice channel";
        this.usage = PREFIX + this.commandName;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_AND_CONNECTED};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            LoopCommand.looped.replace(event.getGuild().getIdLong(), false);
            LoopQueueCommand.looped.replace(event.getGuild().getIdLong(), false);
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            player.stopTrack();
            if (!player.getListener().getTracks().isEmpty()) player.getListener().getTracks().clear();
            event.reply("Stopped by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
