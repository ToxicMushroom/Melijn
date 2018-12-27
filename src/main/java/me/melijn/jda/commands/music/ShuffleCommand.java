package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        this.commandName = "shuffle";
        this.description = "Shuffles the order of the tracks in the queue";
        this.usage = PREFIX + commandName;
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.aliases = new String[]{"randomize"};
        this.id = 26;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = AudioLoader.getManagerInstance().getPlayer(event.getGuild());
            player.getTrackManager().shuffle();
            event.reply("The queue has been **shuffled** by **" + event.getFullAuthorName() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
