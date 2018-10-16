package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import static me.melijn.jda.Melijn.PREFIX;

public class SeekCommand extends Command {

    public SeekCommand() {
        this.commandName = "seek";
        this.description = "seek to the parts of the song that you like :)";
        this.usage = PREFIX + this.commandName + " [hh:mm:ss]";
        this.aliases = new String[]{"skipx", "position"};
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            if (args.length == 1 && args[0].isBlank()) args = new String[0];
            AudioTrack track = MusicManager.getManagerInstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
            if (track != null) {
                if (args.length != 0 && !args[0].isBlank()) {
                    if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                        if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                            long millis = Helpers.parseTimeFromArgs(args);
                            if (millis != -1) {
                                track.setPosition(millis);
                                event.reply("The position of the song has been changed to **" +
                                        (track.getPosition() > track.getDuration() ? Helpers.getDurationBreakdown(track.getDuration()) : Helpers.getDurationBreakdown(track.getPosition())) + "/" +
                                        Helpers.getDurationBreakdown(track.getDuration()) + "** by **" + event.getFullAuthorName() + "**");
                            } else MessageHelper.sendUsage(this, event);
                        } else {
                            event.reply("You have to be in the same voice channel as me to seek");
                        }
                    } else {
                        event.reply("I'm not in a voiceChannel");
                    }
                } else {
                    event.reply("The current position is **" + Helpers.getDurationBreakdown(track.getPosition()) + "/" + Helpers.getDurationBreakdown(track.getDuration()) + "**");
                }
            } else {
                event.reply("There are no songs playing at the moment.");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
