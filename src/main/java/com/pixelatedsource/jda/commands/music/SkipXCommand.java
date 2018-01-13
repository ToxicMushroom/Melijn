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
        this.description = "Skip to the parts of a song you like :)";
        this.usage = PREFIX + this.commandName + " <xx:xx>";
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
                if (args.length < 2 && player != null) {
                    event.reply("Current progress of the song: `" + Helpers.getDurationBreakdown(player.getPosition()) + "`/ `" + Helpers.getDurationBreakdown(player.getDuration()) + "`");
                    return;
                }
                int seconds;
                if (args.length < 2) seconds = 0;
                else try {
                    if (args[0] == null || args[0].equalsIgnoreCase("")) args[0] = "0";
                    Integer.parseInt(args[0]);
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    event.reply("Usage: " + PREFIX + this.commandName + " 1:10");
                    e.addSuppressed(e.getCause());
                    return;
                }
                if (player != null) player.setPosition(Integer.parseInt(args[0]) * 60000 + seconds * 1000);
                else event.reply("Their are no songs playing at the moment.");
            }
        }
    }
}
