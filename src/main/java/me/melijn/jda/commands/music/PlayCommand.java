package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class PlayCommand extends Command {

    public PlayCommand() {
        this.commandName = "play";
        this.description = "plays a song or adds it to the queue";
        this.usage = PREFIX + this.commandName + " [sc] <songname | link>";
        this.extra = "You only have to use sc if you want to search on soundcloud";
        this.aliases = new String[]{"p"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT};
    }

    private List<String> providers = new ArrayList<>(Arrays.asList("yt", "sc", "link", "youtube", "soundcloud"));
    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            boolean access = Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".*", 1);
            VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
            String args[] = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
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
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".sc", 0) || access) {
                        if (SPlayCommand.isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                        manager.loadTrack(event.getTextChannel(), "scsearch:" + songName, event.getAuthor(), false);
                    } else {
                        event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                    }
                    break;
                case "link":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || access) {
                        if (SPlayCommand.isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                        manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                    } else {
                        event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                    }
                    break;
                default:
                    if (songName.contains("https://") || songName.contains("http://")) {
                        songName = songName.replaceAll("\\s+", "");
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || access) {
                            if (SPlayCommand.isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                            if (songName.contains("open.spotify.com")) {
                                if (songName.matches("https://open.spotify.com/track/\\S+")) {
                                    JSONObject object = WebUtils.getWebUtilsInstance().getInfoFromSpotifyUrl(songName);
                                    if (object.has("name"))
                                        manager.loadTrack(event.getTextChannel(), "ytsearch:" + object.get("name"), event.getAuthor(), false);
                                    else event.reply("Could not retrieve data from url");
                                } else {
                                    event.reply("We only support spotify track (no albums) so make sure your url looks like below\n-> (%id% is a long string of nonsense) https://open.spotify.com/track/%id%");
                                    return;
                                }
                            } else
                                manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                        } else {
                            event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                        }
                    } else {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || access) {
                            if (SPlayCommand.isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                            manager.loadTrack(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor(), false);
                        } else {
                            event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                        }
                    }
                    break;
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}