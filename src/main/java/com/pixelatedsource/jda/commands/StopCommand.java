package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS;

public class StopCommand extends Command {

    public StopCommand() {
        this.guildOnly = true;
        this.name = "stop";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name;
        this.botPermissions = new Permission[] {MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            PixelatedBot.looped.put(event.getGuild(), false);
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            player.stopTrack();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Helpers.EmbedColor);
            eb.setTitle("Stopped");
            eb.setDescription("**I stopped playing music and left the voicechannel.**");
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.getTextChannel().sendMessage(eb.build()).queue();
        }
    }
}
