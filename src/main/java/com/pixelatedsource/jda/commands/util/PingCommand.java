package com.pixelatedsource.jda.commands.util;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.awt.*;

public class PingCommand extends Command {

    public PingCommand() {
        this.name = "ping";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name;
        this.botPermissions = new Permission[]{ Permission.MESSAGE_EMBED_LINKS };
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.CYAN);
        eb.setDescription("Pong! `" + event.getJDA().getPing() + "`");
        eb.setThumbnail("https://iminco.nl/wp-content/uploads/kip-1024x512.jpg");
        eb.setTitle("Ping", "http://www.pixelnetwork.be/videos/bingbingbong.mp4");
        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
        event.reply(eb.build());
    }
}
