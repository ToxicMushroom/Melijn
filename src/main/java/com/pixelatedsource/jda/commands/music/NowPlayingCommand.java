package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import okhttp3.HttpUrl;

import java.util.regex.Pattern;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class NowPlayingCommand extends Command {

    public NowPlayingCommand() {
        this.commandName = "np";
        this.description = "Show you the song that the bot is playing";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"nowplaying", "playing", "nplaying", "nowp"};
        this.category = Category.MUSIC;
    }

    private MusicManager musicManager = MusicManager.getManagerinstance();
    private Pattern youtubePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtube\\.com))/watch(.*?)");
    private Pattern youtuBePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtu\\.be/))(.*?)");

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                Guild guild = event.getGuild();
                MusicPlayer audioPlayer = musicManager.getPlayer(guild);
                if (audioPlayer != null) {
                    AudioTrack track = audioPlayer.getAudioPlayer().getPlayingTrack();
                    String s = audioPlayer.getAudioPlayer().isPaused() ? "paused" : "playing";
                    if (track == null) event.reply("There are no songs playing at the moment.");
                    else {
                        String url = track.getInfo().uri;
                        HttpUrl httpUrl = HttpUrl.parse(url);
                        String thumbnailUrl = null;
                        if (httpUrl != null) {
                            if (url.matches(youtubePattern.pattern())) {
                                if (httpUrl.queryParameter("v") != null) {
                                    thumbnailUrl = "https://img.youtube.com/vi/" + httpUrl.queryParameter("v") + "/hqdefault.jpg";
                                }
                            } else if (url.matches(youtuBePattern.pattern())) {
                                thumbnailUrl = "https://img.youtube.com/vi/" + url.replaceFirst(youtuBePattern.pattern(), "") + "/hqdefault.jpg";
                            }
                        }
                        Emote emote = event.getJDA().getEmoteById("445154561313865728");
                        String looped = LoopCommand.looped.getOrDefault(guild.getId(), false) ? ":repeat: " : "";
                        event.reply(new EmbedBuilder()
                                .setTitle("Now " + s)
                                .setColor(Helpers.EmbedColor)
                                .setThumbnail(thumbnailUrl)
                                .addField("title:", "[**" + track.getInfo().title + "**](" + track.getInfo().uri + ")", false)
                                .addField("status:", looped + (s.equalsIgnoreCase("playing") ? ":arrow_forward:" : ":pause_button:"), false)
                                .addField("progress:", MessageHelper.progressBar(track, emote), false)
                                .setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon()).build());
                    }
                } else {
                    event.reply("There are not songs playing.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
