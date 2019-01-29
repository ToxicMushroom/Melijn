package me.melijn.jda.commands.music;

import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class ClearCommand extends Command {

    public ClearCommand() {
        this.commandName = "clear";
        this.description = "Clears the queue";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"cls"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 62;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            MusicPlayer player = event.getClient().getMelijn().getLava().getAudioLoader().getPlayer(event.getGuild());
            player.getTrackManager().clear();
            event.reply("The queue has been cleared by **" + event.getFullAuthorName() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
