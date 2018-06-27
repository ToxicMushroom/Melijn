package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PitchCommand extends Command {

    public PitchCommand() {
        this.commandName = "pitch";
        this.description = "Change the playback pitch of bot";
        this.usage = PREFIX + this.commandName + " <default | 0.0-10.0>";
        this.category = Category.MUSIC;
        this.extra = "1.0 is normal pitch";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                        if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                            if (args[0].matches("[0-9]|[0-9][0-9]|100|[0-9]?[0-9].[0-9]?[0-9]?[0-9]")) {
                                if (player != null && player.getAudioPlayer().getPlayingTrack() != null) {
                                    if (Double.parseDouble(args[0]) > 0) {
                                        player.setPitch(Double.parseDouble(args[0]));
                                        player.updateFilters();
                                        event.reply("The pitch has been set to **" + args[0] + "** by **" + event.getFullAuthorName() + "**");
                                    } else {
                                        event.reply("Pitch cannot be 0 (just set volume to 0 you'll get the same effect hha");
                                    }
                                } else {
                                    event.reply("There is no music playing");
                                }
                            } else if (args[0].equalsIgnoreCase("default")) {
                                player.setPitch(1);
                                player.updateFilters();
                                event.reply("The pitch has been set to **" + 1 + "** by **" + event.getFullAuthorName() + "**");
                            } else {
                                MessageHelper.sendUsage(this, event);
                            }
                        } else {
                            event.reply("You have to be in the same voice channel as me to change the pitch");
                        }
                    } else {
                        event.reply("I'm not in a voiceChannel");
                    }
                } else {
                    event.reply("The configured pitch is **" + player.getSpeed() + "**");
                }

            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
