package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class QueueCommand extends Command {

    public QueueCommand() {
        this.commandName = "queue";
        this.description = "Shows you a list of all songs wich will play in the future.";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"q", "list"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    private MusicManager manager = MusicManager.getManagerinstance();

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
            String loopedQueue = LoopQueueCommand.looped.getOrDefault(guild.getIdLong(), false) ? " :repeat:" : "";
            String looped = LoopCommand.looped.getOrDefault(guild.getIdLong(), false) ? " :arrows_counterclockwise:" : "";
            lijst.add("Status: " + (player.getAudioPlayer().isPaused() ? ":pause_button:" : ":arrow_forward:") + looped + loopedQueue);
            lijst.add("Queue size: **" + (lijst.size() - 1) + "** tracks");
            lijst.add("Queue length: **" + Helpers.getDurationBreakdown(totalqueuelength) + "**");
            StringBuilder builder = new StringBuilder();
            for (String s : lijst) {
                builder.append(s).append("\n");
            }
            if (builder.toString().length() > 1800) {
                int part = 1;
                builder = new StringBuilder();
                for (String s : lijst) {
                    if (builder.toString().length() + s.length() > 1800) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Queue part " + part);
                        eb.setColor(Helpers.EmbedColor);
                        eb.setDescription(builder.toString());
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        event.reply(eb.build());
                        builder = new StringBuilder();
                        part++;
                    }
                    builder.append(s).append("\n");
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Queue part " + part);
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Queue");
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
