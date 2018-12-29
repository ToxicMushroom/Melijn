package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.LavalinkPlayer;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;

public class NowPlayingCommand extends Command {

    private AudioLoader audioLoader = AudioLoader.getManagerInstance();
    public final static Pattern youtubePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtube\\.com))/watch(.*?)");
    public final static Pattern youtuBePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtu\\.be/))(.*?)");

    public NowPlayingCommand() {
        this.commandName = "np";
        this.description = "Shows you the current track";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"nowplaying", "playing", "nplaying", "nowp"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.id = 72;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            Guild guild = event.getGuild();
            LavalinkPlayer audioPlayer = audioLoader.getPlayer(guild).getAudioPlayer();
            AudioTrack track = audioPlayer.getPlayingTrack();
            String s = audioPlayer.isPaused() ? "paused" : "playing";
            if (track == null) {
                event.reply("There are no tracks being played");
                return;
            }

            String loopedQueue = LoopQueueCommand.looped.contains(guild.getIdLong()) ? " \uD83D\uDD01" : "";
            String looped = LoopCommand.looped.contains(guild.getIdLong()) ? " \uD83D\uDD04" : "";
            event.reply(new Embedder(event.getGuild())
                    .setTitle("Now " + s)
                    .setThumbnail(MessageHelper.getThumbnailURL(track.getInfo().uri))
                    .setDescription("[**" + track.getInfo().title + "**](" + track.getInfo().uri + ")")
                    .addField("status:", (s.equalsIgnoreCase("playing") ? "\u25B6" : "\u23F8") + looped + loopedQueue, false)
                    .addField("progress:", MessageHelper.progressBar(audioPlayer), false)
                    .setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon()).build());

        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
