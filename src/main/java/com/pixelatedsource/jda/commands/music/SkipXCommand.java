package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SkipXCommand extends Command {

    public SkipXCommand() {
        this.commandName = "skipx";
        this.description = "Skip to the parts of the song that you like :)";
        this.usage = PREFIX + this.commandName + " [xx:xx:xx]";
        this.aliases = new String[]{"seek"};
        this.category = Category.MUSIC;
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().replaceFirst(":", " ").split("\\s+");
                AudioTrack player = manager.getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();
                if (player == null) {
                    event.reply("There are no songs playing at the moment.");
                    return;
                }
                long millis = -1;
                switch (args.length) {
                    case 0: {
                        event.reply("Current progress of the song: `**" + Helpers.getDurationBreakdown(player.getPosition()) + "** / **" + Helpers.getDurationBreakdown(player.getDuration()) + "**");
                        break;
                    }
                    case 1: {
                        if (args[0].matches("(\\d)|(\\d\\d)")) {
                            millis = 1000 * Byte.parseByte(args[0]);
                        }
                        break;
                    }
                    case 2: {
                        if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)")) {
                            millis = 1000 * Byte.parseByte(args[0]) + 60000 * Byte.parseByte(args[1]);
                        }
                        break;
                    }
                    case 3: {
                        if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                            millis = 1000 * Byte.parseByte(args[0]) + 60000 * Byte.parseByte(args[1]) + 3600000 * Byte.parseByte(args[2]);
                        }
                        break;
                    }
                    default: {
                        if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                            millis = 1000 * Byte.parseByte(args[0]) + 60000 * Byte.parseByte(args[1]) + 3600000 * Byte.parseByte(args[2]);
                        }
                        break;
                    }
                }
                if (millis != -1)
                    player.setPosition(millis);
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
