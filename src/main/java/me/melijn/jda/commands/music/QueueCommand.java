package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static me.melijn.jda.Melijn.PREFIX;

public class QueueCommand extends Command {

    public QueueCommand() {
        this.commandName = "queue";
        this.description = "Shows you a list of all tracks in queue.";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"q", "list"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.id = 59;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            Guild guild = event.getGuild();
            MusicPlayer player = event.getClient().getMelijn().getLava().getAudioLoader().getPlayer(guild);
            if (player.getTrackManager().getTrackSize() == 0 && player.getAudioPlayer().getPlayingTrack() == null) {
                event.reply("Nothing here..");
                return;
            }
            Queue<AudioTrack> tracks = player.getTrackManager().getTracks();
            List<String> list = new ArrayList<>(); //This is where normal people use a stringbuilder
            int position = 0;
            double queueLength = 0;
            AudioTrack playingTrack = player.getAudioPlayer().getPlayingTrack();
            if (playingTrack != null) {
                queueLength = playingTrack.getDuration() - playingTrack.getPosition();
                list.add("[#" + position + "](" + playingTrack.getInfo().uri + ") - `Now playing:` " + playingTrack.getInfo().title.replaceAll("\\*", "\\\\*") + " `" + event.getMessageHelper().getDurationBreakdown(playingTrack.getInfo().length) + "`");
            }
            for (AudioTrack track : tracks) {
                position++;
                queueLength += track.getDuration();
                list.add("[#" + position + "](" + track.getInfo().uri + ") - " + track.getInfo().title.replaceAll("\\*", "\\\\*") + " `" + event.getMessageHelper().getDurationBreakdown(track.getInfo().length) + "`");
            }
            String loopedQueue = event.getVariables().loopedQueues.contains(guild.getIdLong()) ? " \uD83D\uDD01" : "";
            String looped = event.getVariables().looped.contains(guild.getIdLong()) ? " \uD83D\uDD04" : "";
            list.add("Status: " + (player.getAudioPlayer().isPaused() ? "\u23F8" : "\u25B6") + looped + loopedQueue);
            list.add("Queue size: **" + (list.size() - 1) + "** tracks");
            list.add("Queue length: **" + event.getMessageHelper().getDurationBreakdown(queueLength) + "**");
            StringBuilder builder = new StringBuilder();
            for (String s : list) {
                builder.append(s).append("\n");
            }
            if (builder.length() > 1800) {
                int part = 1;
                builder = new StringBuilder();
                for (String s : list) {
                    if (builder.length() + s.length() > 1800) {
                        EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                        eb.setTitle("Queue part " + part);
                        eb.setDescription(builder.toString());
                        eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                        event.reply(eb.build());
                        builder = new StringBuilder();
                        part++;
                    }
                    builder.append(s).append("\n");
                }
                EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                eb.setTitle("Queue part " + part);
                eb.setDescription(builder.toString());
                eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                event.reply(eb.build());
            } else {
                EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                eb.setTitle("Queue");
                eb.setDescription(builder.toString());
                eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                event.reply(eb.build());
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
