package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ClearCommand extends Command {

    public ClearCommand() {
        this.commandName = "clear";
        this.description = "Clears the queue";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"cls"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Helpers.EmbedColor);
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                if (player.getListener().getTracks().isEmpty()) {
                    eb.setTitle("But...");
                    eb.setDescription("**There are no songs to remove.**");
                } else {
                    player.getListener().getTracks().clear();
                    eb.setTitle("Cleared");
                    eb.setDescription("**I cleared the queue i hope that you aren't mad at me :(. i'm a __good__ pet.**");
                }
                event.reply(eb.build());
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
