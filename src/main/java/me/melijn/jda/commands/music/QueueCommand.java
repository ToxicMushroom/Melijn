package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static me.melijn.jda.Melijn.PREFIX;

public class QueueCommand extends Command {

    private MusicManager manager = MusicManager.getManagerInstance();

    public QueueCommand() {
        this.commandName = "queue";
        this.description = "Shows you a list of all tracks in queue.";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"q", "list"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            Guild guild = event.getGuild();
            MusicPlayer player = manager.getPlayer(guild);
            if (player.getListener().getTrackSize() == 0 && player.getAudioPlayer().getPlayingTrack() == null) {
                event.reply("Nothing here..");
                return;
            }
            BlockingQueue<AudioTrack> tracks = player.getListener().getTracks();
            List<String> lijst = new ArrayList<>();
            int i = 0;
            long totalqueuelength = 0;
            if (player.getAudioPlayer().getPlayingTrack() != null) {
                totalqueuelength = player.getAudioPlayer().getPlayingTrack().getDuration() - player.getAudioPlayer().getPlayingTrack().getPosition();
                lijst.add(String.valueOf("[#" + i + "](" + player.getAudioPlayer().getPlayingTrack().getInfo().uri + ") - `Now playing:` " + player.getAudioPlayer().getPlayingTrack().getInfo().title + " `" + Helpers.getDurationBreakdown(player.getAudioPlayer().getPlayingTrack().getInfo().length) + "`"));
            }
            for (AudioTrack track : tracks) {
                i++;
                totalqueuelength += track.getDuration();
                lijst.add(String.valueOf("[#" + i + "](" + track.getInfo().uri + ") - " + track.getInfo().title + " `" + Helpers.getDurationBreakdown(track.getInfo().length) + "`"));
            }
            String loopedQueue = LoopQueueCommand.looped.contains(guild.getIdLong()) ? " \uD83D\uDD01" : "";
            String looped = LoopCommand.looped.contains(guild.getIdLong()) ? " \uD83D\uDD04" : "";
            lijst.add("Status: " + (player.getAudioPlayer().isPaused() ? "\u23F8" : "\u25B6") + looped + loopedQueue);
            lijst.add("Queue size: **" + (lijst.size() - 1) + "** tracks");
            lijst.add("Queue length: **" + Helpers.getDurationBreakdown(totalqueuelength) + "**");
            StringBuilder builder = new StringBuilder();
            for (String s : lijst) {
                builder.append(s).append("\n");
            }
            if (builder.length() > 1800) {
                int part = 1;
                builder = new StringBuilder();
                for (String s : lijst) {
                    if (builder.length() + s.length() > 1800) {
                        EmbedBuilder eb = new Embedder(event.getGuild());
                        eb.setTitle("Queue part " + part);
                        eb.setDescription(builder.toString());
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        event.reply(eb.build());
                        builder = new StringBuilder();
                        part++;
                    }
                    builder.append(s).append("\n");
                }
                EmbedBuilder eb = new Embedder(event.getGuild());
                eb.setTitle("Queue part " + part);
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            } else {
                EmbedBuilder eb = new Embedder(event.getGuild());
                eb.setTitle("Queue");
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
