package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;

import java.awt.*;

public class PingCommand extends Command {

    public PingCommand() {
        this.name = "ping";
        this.help = "Shows you the ping of the bot";
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
