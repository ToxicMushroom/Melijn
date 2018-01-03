package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class ResumeCommand extends Command {

    public ResumeCommand() {
        this.name = "resume";
        this.guildOnly = true;
        this.help = "Resumes the paused queue -> Usage: " + PixelatedBot.PREFIX + this.name;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name)) {
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            VoiceChannel voiceChannel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
            event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
            player.resumeTrack();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Helpers.EmbedColor);
            eb.setTitle("Resumed");
            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
            eb.setDescription("**Thank you for resuming my queue. I appreciate that.**");
            event.getTextChannel().sendMessage(eb.build()).queue();
        }
    }
}
