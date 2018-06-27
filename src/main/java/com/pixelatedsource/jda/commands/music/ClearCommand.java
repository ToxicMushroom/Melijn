package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ClearCommand extends Command {

    public ClearCommand() {
        this.commandName = "clear";
        this.description = "Clears the queue";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"cls"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                        if (player.getAudioPlayer().getPlayingTrack() != null)
                            player.getAudioPlayer().getPlayingTrack().stop();
                        if (!player.getListener().getTracks().isEmpty()) player.getListener().getTracks().clear();
                        event.reply("The queue has been cleared by **" + event.getFullAuthorName() + "**");
                    } else {
                        event.reply("You have to be in the same voice channel as me to clear tracks");
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
