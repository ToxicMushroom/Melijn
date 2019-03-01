package me.melijn.jda.commands.music;

import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static me.melijn.jda.Melijn.PREFIX;

public class PlayCommand extends Command {


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
        boolean access = event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".*", 1);
        VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            event.sendUsage(this, event);
            return;
        }
        Lava lava = event.getClient().getMelijn().getLava();
        String songName;
        StringBuilder sb = new StringBuilder();
        event.getMessageHelper().argsToSongName(args, sb, event.getVariables().providers);
        songName = sb.toString();
        switch (args[0].toLowerCase()) {
            case "sc":
            case "soundcloud":
                if (!event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".sc", 0) && !access) {
                    event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                    return;
                }
                if (lava.tryToConnectToVC(event, guild, senderVoiceChannel))
                    lava.getAudioLoader().loadTrack(event.getTextChannel(), "scsearch:" + songName, event.getAuthor(), false);

                break;
            case "link":
                if (!event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".link", 0) && !access) {
                    event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                    return;
                }
                if (lava.tryToConnectToVC(event, guild, senderVoiceChannel))
                    lava.getAudioLoader().loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);

                break;
            default:
                if (songName.contains("https://") || songName.contains("http://")) {
                    if (!event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".link", 0) && !access) {
                        event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                        return;
                    }
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    if (songName.contains("open.spotify.com")) spotiSearch(lava.getAudioLoader(), event, songName);
                    else
                        lava.getAudioLoader().loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                } else {
                    if (!event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".yt", 0) && !access) {
                        event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                        return;
                    }
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    if (songName.matches("spotify:(.*)")) spotiSearch(lava.getAudioLoader(), event, songName);
                    else
                        lava.getAudioLoader().loadTrack(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor(), false);
                }
                break;
        }
    }

    private void spotiSearch(AudioLoader audioLoader, CommandEvent event, String url) {
        event.getWebUtils().getTracksFromSpotifyUrl(url,
                (track) -> audioLoader.loadSpotifyTrack(event.getTextChannel(), "ytsearch:" + track.getName(), track.getArtists(), track.getDurationMs()),
                (tracks) -> audioLoader.loadSpotifyPlaylist(event.getTextChannel(), tracks),
                (tracksa) -> audioLoader.loadSpotifyAlbum(event.getTextChannel(), tracksa),
                (rip) -> event.reply("Could not retrieve data from spotify"));
    }
}