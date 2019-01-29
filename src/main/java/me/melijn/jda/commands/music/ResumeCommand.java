package me.melijn.jda.commands.music;

import me.melijn.jda.audio.Lava;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static me.melijn.jda.Melijn.PREFIX;

public class ResumeCommand extends Command {

    public ResumeCommand() {
        this.commandName = "resume";
        this.description = "Resumes the paused track when paused";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"unpause"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.category = Category.MUSIC;
        this.id = 64;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            Lava lava = event.getClient().getMelijn().getLava();
            MusicPlayer player = lava.getAudioLoader().getPlayer(event.getGuild());
            VoiceChannel voiceChannel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
            if (voiceChannel == null) voiceChannel = event.getGuild().getSelfMember().getVoiceState().getChannel();
            if (voiceChannel != null) {
                if (!lava.tryToConnectToVC(event, event.getGuild(), voiceChannel)) return;

                player.resumeTrack();
                if (player.getAudioPlayer().getPlayingTrack() == null && player.getTrackManager().getTrackSize() > 0)
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
