package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SeekCommand extends Command {

    public SeekCommand() {
        this.commandName = "seek";
        this.description = "seek to the parts of the song that you like :)";
        this.usage = PREFIX + this.commandName + " [xx:xx:xx]";
        this.aliases = new String[]{"skipx", "position"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            if (args.length == 1 && args[0].equalsIgnoreCase("")) args = new String[0];
            AudioTrack track = MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
            if (track != null) {
                if (args.length != 0 && !args[0].equalsIgnoreCase("")) {
                    if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                        if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                            long millis = Helpers.parseTimeFromArgs(args);
                            if (millis != -1) {
                                track.setPosition(millis);
                                event.reply("The position of the song has been changed to **" + Helpers.getDurationBreakdown(track.getPosition()) + "/" + Helpers.getDurationBreakdown(track.getDuration()) + "** by **" + event.getFullAuthorName() + "**");
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
