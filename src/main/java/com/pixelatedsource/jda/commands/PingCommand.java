package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        // The API call was successful
        event.reply(eb.build());
    }
}
