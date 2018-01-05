package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;

public class ClearCommand extends Command {

    public ClearCommand() {
        this.name = "clear";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name;
        this.aliases = new String[] {"cls"};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0);
        if (acces) {
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
        }
    }
}
