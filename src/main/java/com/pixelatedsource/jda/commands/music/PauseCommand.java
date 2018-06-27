package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PauseCommand extends Command {

    public PauseCommand() {
        this.commandName = "pause";
        this.description = "pause the queue without stopping or deleting songs";
        this.category = Category.MUSIC;
        this.usage = PREFIX + commandName + " [on/enable/true | off/disable/false]";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                        String[] args = event.getArgs().split("\\s+");
                        if (player.getAudioPlayer().getPlayingTrack() != null || player.getListener().getTrackSize() > 0) {
                            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                                if (player.getAudioPlayer().isPaused()) {
                                    player.getAudioPlayer().setPaused(false);
                                    event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                                } else {
                                    player.getAudioPlayer().setPaused(true);
                                    event.reply("Paused by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                                }
                            } else if (args.length == 1) {
                                switch (args[0]) {
                                    case "on":
                                    case "enable":
                                    case "true":
                                        player.getAudioPlayer().setPaused(true);
                                        event.reply("Paused by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                                        break;
                                    case "off":
                                    case "disable":
                                    case "false":
                                        player.getAudioPlayer().setPaused(false);
                                        event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                                        break;
                                    case "info":
                                        String s = player.getPaused() ? "paused" : "playing";
                                        event.reply("The music is currently **" + s + "**.");
                                        break;
                                    default:
                                        MessageHelper.sendUsage(this, event);
                                        break;
                                }
                            }
                        } else {
                            event.reply("There are no songs playing at the moment");
                        }
                    } else {
                        event.reply("You have to be in the same voice channel as me to pause tracks");
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
