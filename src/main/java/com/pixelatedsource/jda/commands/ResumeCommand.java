package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ResumeCommand extends Command {

    public ResumeCommand() {
        this.name = "resume";
        this.help = "resumes the paused song";
    }

    @Override
    protected void execute(CommandEvent event) {
        MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
        VoiceChannel voiceChannel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
        event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
        player.resumeTrack();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Helpers.EmbedColor);
        eb.setTitle("Resumed");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        eb.setDescription("**Thank you for resuming my queue. I appreciate that very well.**");
        event.getTextChannel().sendMessage(eb.build()).queue();
    }
}
