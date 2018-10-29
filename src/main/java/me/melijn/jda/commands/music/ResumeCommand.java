package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static me.melijn.jda.Melijn.PREFIX;

public class ResumeCommand extends Command {

    public ResumeCommand() {
        this.commandName = "resume";
        this.description = "Resume the paused song when paused";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"unpause"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            VoiceChannel voiceChannel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
            if (voiceChannel == null) voiceChannel = event.getGuild().getSelfMember().getVoiceState().getChannel();
            if (voiceChannel != null) {
                if (SPlayCommand.isNotConnectedOrConnecting(event, event.getGuild(), voiceChannel)) return;

                player.resumeTrack();
                if (player.getAudioPlayer().getPlayingTrack() == null && player.getListener().getTrackSize() > 0)
                    player.skipTrack();
                event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
            } else {
                event.reply("You or I have to be in a voice channel to resume");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
