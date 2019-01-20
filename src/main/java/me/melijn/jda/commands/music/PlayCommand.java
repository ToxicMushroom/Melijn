package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class PlayCommand extends Command {

    final static List<String> providers = Arrays.asList("yt", "sc", "link", "youtube", "soundcloud");
    private AudioLoader manager = AudioLoader.getManagerInstance();
    private Lava lava = Lava.lava;

    public PlayCommand() {
        this.commandName = "play";
        this.description = "Plays a track or adds it to the queue";
        this.usage = PREFIX + commandName + " [sc] <songname | link>";
        this.extra = "You only have to use sc if you want to search on soundcloud";
        this.aliases = new String[]{"p"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT};
        this.id = 58;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        boolean access = Helpers.hasPerm(guild.getMember(event.getAuthor()), commandName + ".*", 1);
        VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            MessageHelper.sendUsage(this, event);
            return;
        }
        String songName;
        StringBuilder sb = new StringBuilder();
        SPlayCommand.argsToSongName(args, sb, providers);
        songName = sb.toString();
        switch (args[0].toLowerCase()) {
            case "sc":
            case "soundcloud":
                if (!Helpers.hasPerm(guild.getMember(event.getAuthor()), commandName + ".sc", 0) && !access) {
                    event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                    return;
                }
                if (lava.tryToConnectToVC(event, guild, senderVoiceChannel))
                    manager.loadTrack(event.getTextChannel(), "scsearch:" + songName, event.getAuthor(), false);

                break;
            case "link":
                if (!Helpers.hasPerm(guild.getMember(event.getAuthor()), commandName + ".link", 0) && !access) {
                    event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                    return;
                }
                if (lava.tryToConnectToVC(event, guild, senderVoiceChannel))
                    manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);

                break;
            default:
                if (songName.contains("https://") || songName.contains("http://")) {
                    if (!Helpers.hasPerm(guild.getMember(event.getAuthor()), commandName + ".link", 0) && !access) {
                        event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                        return;
                    }
                    songName = songName.replaceAll("\\s+", "");
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    if (songName.contains("open.spotify.com")) spotiSearch(event, songName);
                    else manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                } else {
                    if (!Helpers.hasPerm(guild.getMember(event.getAuthor()), commandName + ".yt", 0) && !access) {
                        event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                        return;
                    }
                    songName = songName.replaceAll("\\s+", "");
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    if (songName.matches("spotify:(.*)")) spotiSearch(event, songName);
                    else manager.loadTrack(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor(), false);
                }
                break;
        }
    }

    private void spotiSearch(CommandEvent event, String url) {
        WebUtils.getWebUtilsInstance().getTracksFromSpotifyUrl(url,
                (track) -> manager.loadSpotifyTrack(event.getTextChannel(), "ytsearch:" + track.getName(), track.getArtists(), track.getDurationMs()),
                (tracks) -> manager.loadSpotifyPlaylist(event.getTextChannel(), tracks),
                (tracksa) -> manager.loadSpotifyAlbum(event.getTextChannel(), tracksa),
                (rip) -> event.reply("Could not retrieve data from spotify"));
    }
}