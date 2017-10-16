package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class PlayCommand extends Command {
    public PlayCommand() {
        this.name = "play";
        this.help = "play a song with yt or sc";
    }

    MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        VoiceChannel sendervoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String args[] = event.getArgs().split(" ");
        if (args.length == 0 || args[0].equalsIgnoreCase("")) {//no args -> usage:
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Some info for new people");
            eb.setColor(Helpers.EmbedColor);
            eb.setDescription(PixelatedBot.PREFIX + "play [yt|sc|link] <Songname>");
            eb.addField("Legenda","[] = optional" +
                    "| = or" +
                    "<> = needed", true);
            eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
            event.reply(eb.build());
            return;
        }

        args[0] = args[0].toLowerCase(); //makes it so there will be less to check
        String songname = Arrays.toString(args).replaceFirst("sc", "").replaceFirst("yt", "").replaceFirst("soundcloud", "").replaceFirst("youtube", "").replaceFirst("link", "");

        if (sendervoiceChannel == null) {//if the user isn't in a voice channel in the same guild
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("I'm a pet so i follow you everywhere :3");
            eb.setColor(Helpers.EmbedColor);
            eb.setDescription("PS: you need to join a voice channel then when you use the command i'll party with you");
            eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
            event.reply(eb.build());
            event.replyWarning("");
            return;
        }
        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(sendervoiceChannel);
        if (args[0].equalsIgnoreCase("sc") || args[0].equalsIgnoreCase("soundcloud")) {
            // if statement opens a conection in case of not connected
            manager.loadTrack(event.getTextChannel(), "scsearch:" + songname);
            Helpers.LOG.debug("scsearch");
        } else if (args[0].equalsIgnoreCase("link")) {
            manager.loadTrack(event.getTextChannel(), songname);
            Helpers.LOG.debug("secrets");
        } else {
            manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname);
            Helpers.LOG.debug("YOUTUBE");
        }

    }
}
