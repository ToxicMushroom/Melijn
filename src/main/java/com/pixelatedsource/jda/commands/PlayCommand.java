package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.Arrays;

public class PlayCommand extends Command {

    public PlayCommand() {
        this.name = "play";
        this.guildOnly = true;
        this.help = "Play sounds like BOOM BOOM or wooosh ect.. -> Usage: " + PixelatedBot.PREFIX + this.name + " [yt|sc|link] <songname|link>" +
                "\nYoutube is the default music browser.";
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        VoiceChannel sendervoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String args[] = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].equalsIgnoreCase("")) {//no args -> usage:
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Some info for new people");
            eb.setColor(Helpers.EmbedColor);
            eb.setDescription(PixelatedBot.PREFIX + this.name + " [yt|sc|link] <Songname>");
            eb.addField("Legenda", "[] = optional" +
                    "| = or" +
                    "<> = needed", true);
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.reply(eb.build());
            return;
        }
        args[0] = args[0].toLowerCase();
        String songname = Arrays.toString(args)
                .replaceFirst("sc", "")
                .replaceFirst("yt", "")
                .replaceFirst("soundcloud", "")
                .replaceFirst("youtube", "")
                .replaceFirst("link", "")
                .replaceFirst("looplink", "");
        if (sendervoiceChannel == null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("I'm a pet so i follow you everywhere :3");
            eb.setColor(Helpers.EmbedColor);
            eb.setDescription("PS: you need to join a voice channel then when you use the command then I'll party with you");
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            event.reply(eb.build());
            return;
        }

        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect())
            guild.getAudioManager().openAudioConnection(sendervoiceChannel);
        if (args[0].equalsIgnoreCase("sc") || args[0].equalsIgnoreCase("soundcloud")) {
            if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.name + ".sc"))
                manager.loadTrack(event.getTextChannel(), "scsearch:" + songname);
        } else if (args[0].equalsIgnoreCase("link")) {
            if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.name + ".link"))
                manager.loadTrack(event.getTextChannel(), args[(args.length - 1)]);
        } else if (args[0].equalsIgnoreCase("looplink")) {
            if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.name + ".looplink")) {
                manager.loadTrack(event.getTextChannel(), args[(args.length - 1)]);
                manager.loadTrack(event.getTextChannel(), args[(args.length - 1)]);
                PixelatedBot.looped.put(event.getGuild(), true);
            }
        } else {
            if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.name + ".yt"))
                manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname);
        }
    }
}