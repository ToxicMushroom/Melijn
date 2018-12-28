package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.LavalinkPlayer;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class SeekCommand extends Command {

    public SeekCommand() {
        this.commandName = "seek";
        this.description = "Seeks the part of the track you desire";
        this.usage = PREFIX + commandName + " [hh:mm:ss]";
        this.aliases = new String[]{"skipx", "position"};
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 70;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            LavalinkPlayer player = AudioLoader.getManagerInstance().getPlayer(guild).getAudioPlayer();
            AudioTrack track = player.getPlayingTrack();
            if (track == null) {
                event.reply("There are currently no tracks playing");
                return;
            }
            if (args.length == 0 || args[0].isBlank()) {
                event.reply("The current position is **" + Helpers.getDurationBreakdown(player.getTrackPosition()) + "/" + Helpers.getDurationBreakdown(track.getDuration()) + "**");
                return;
            }
            if (event.getMember().getVoiceState().getChannel() == Lava.lava.getConnectedChannel(guild) || Helpers.hasPerm(event.getMember(), "bypass.sameVoiceChannel", 1)) {
                long millis = Helpers.parseTimeFromArgs(args);
                if (millis == -1) MessageHelper.sendUsage(this, event);
                else {
                    track.setPosition(millis);
                    event.reply("The position of the song has been changed to **" +
                            Helpers.getDurationBreakdown(Math.min(millis, track.getDuration())) + "/" +
                            Helpers.getDurationBreakdown(track.getDuration()) + "** by **" + event.getFullAuthorName() + "**");
                }
            } else {
                event.reply("You have to be in the same voice channel as me to seek");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
