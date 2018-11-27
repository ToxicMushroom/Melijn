package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;

import static me.melijn.jda.Melijn.PREFIX;

public class StopCommand extends Command {

    public StopCommand() {
        this.commandName = "stop";
        this.description = "Stops the queue";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"leave"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            LoopCommand.looped.remove(event.getGuild().getIdLong());
            LoopQueueCommand.looped.remove(event.getGuild().getIdLong());
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            player.stopTrack();
            if (!player.getListener().getTracks().isEmpty()) player.getListener().getTracks().clear();
            event.reply("Stopped by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
