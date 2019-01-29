package me.melijn.jda.commands.music;

import lavalink.client.player.IPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class ForwardCommand extends Command {

    public ForwardCommand() {
        this.commandName = "forward";
        this.description = "Forwards inside the track";
        this.usage = PREFIX + commandName + " [hh:mm:ss]";
        this.extra = "e.g. >forward 11 -> +11s | >forward 1:01 -> +61s | >forward 1:02:01 -> +3721s";
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 13;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            String[] args = event.getArgs().replaceAll(":", " ").split("\\s+");
            IPlayer player = event.getClient().getMelijn().getLava().getAudioLoader().getPlayer(event.getGuild()).getAudioPlayer();
            if (player.getPlayingTrack() == null) {
                event.reply("There are no songs playing at the moment");
                return;
            }
            long millis = event.getHelpers().parseTimeFromArgs(args);
            if (millis == -1) {
                event.getMessageHelper().sendUsage(this, event);
                return;
            }
            millis += player.getTrackPosition();
            player.seekTo(millis);
            event.reply("The position of the song has been changed to **" +
                    event.getMessageHelper().getDurationBreakdown(Math.min(millis, player.getPlayingTrack().getDuration())) + "/" +
                    event.getMessageHelper().getDurationBreakdown(player.getPlayingTrack().getDuration()) + "** by **" + event.getFullAuthorName() + "**");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
