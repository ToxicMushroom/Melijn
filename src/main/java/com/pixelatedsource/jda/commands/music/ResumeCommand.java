package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ResumeCommand extends Command {

    public ResumeCommand() {
        this.commandName = "resume";
        this.description = "Resume the paused song when paused";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"unpause"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            VoiceChannel voiceChannel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
            event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
            player.resumeTrack();
            event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
        }
    }
}
