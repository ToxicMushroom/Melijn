package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;
import static com.pixelatedsource.jda.blub.Need.GUILD;

public class VolumeCommand extends Command {

    public VolumeCommand() {
        this.commandName = "volume";
        this.usage = PREFIX + this.commandName + " <0-100>";
        this.description = "Change or view the volume of the music for everyone";
        this.aliases = new String[]{"vol"};
        this.needs = new Need[]{GUILD};
        this.category = Category.MUSIC;
    }


    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String args[] = event.getArgs().split("\\s+");
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            int volume;
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                event.reply("Current volume: **" + Math.round(player.getAudioPlayer().getVolume() / 1.5) + "%**");
                return;
            }
            try {
                volume = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                MessageHelper.sendUsage(this, event);
                return;
            }
            if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                    if (volume >= 0 && volume <= 100) {
                        player.getAudioPlayer().setVolume(Integer.parseInt(String.valueOf(Math.round(volume * 1.5))));
                        event.reply("Volume has been set to **" + String.valueOf(Math.round((double) volume)) + "%**");
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                } else {
                    event.reply("You have to be in the same voice channel as me to change my volume");
                }
            } else {
                event.reply("I'm not in a voiceChannel");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}

