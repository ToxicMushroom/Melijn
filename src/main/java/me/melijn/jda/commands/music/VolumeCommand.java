package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.Need.GUILD;

public class VolumeCommand extends Command {

    public VolumeCommand() {
        this.commandName = "volume";
        this.usage = PREFIX + commandName + " <0-1000>";
        this.description = "Changes the volume of tracks";
        this.aliases = new String[]{"vol"};
        this.extra = "default: 100 (over 100 will cause distortion)";
        this.needs = new Need[]{GUILD};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            int volume;
            if (args.length == 0 || args[0].isEmpty()) {
                event.reply("Current volume: **" + player.getAudioPlayer().getVolume() + "**");
            } else if ((!Helpers.voteChecks || Melijn.mySQL.getVotesObject(event.getAuthorId()).getLong("streak") > 0)) {
                if (args[0].matches("[0-9]{1,3}|1000")) {
                    volume = Integer.parseInt(args[0]);
                    if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                        if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                            player.getAudioPlayer().setVolume(volume);
                            event.reply("Volume has been set to **" + volume + "**");
                        } else {
                            event.reply("You have to be in the same voice channel as me to change my volume");
                        }
                    } else {
                        event.reply("I'm not in a voiceChannel");
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("Sorry this command takes a lot of CPU usage\nYou can still use this command if you support me by voting each day `>vote`\nor you can just right click my name and use the volume slider");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}

