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

public class ForwardCommand extends Command {

    public ForwardCommand() {
        this.commandName = "forward";
        this.description = "forward inside the song (20s/2m50s + 5s -> 25s/2m50s)";
        this.usage = PREFIX + commandName + " [xx:xx:xx]";
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
                        if (args.length == 1 && args[0].equalsIgnoreCase("")) args = new String[0];
                        AudioTrack player = MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
                        if (player != null) {
                            long millis = Helpers.parseTimeFromArgs(args);
                            if (millis != -1) {
                                player.setPosition(millis + player.getPosition());
                                event.reply("The position of the song has been changed to **" + Helpers.getDurationBreakdown(player.getPosition()) + "/" + Helpers.getDurationBreakdown(player.getDuration()) + "** by **" + event.getFullAuthorName() + "**");
                            } else MessageHelper.sendUsage(this, event);
                        } else {
                            event.reply("There are no songs playing at the moment");
                        }
                    } else {
                        event.reply("You have to be in the same voice channel as me to forward tracks");
                    }
                } else {
                    event.reply("I'm not in a voiceChannel");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
