package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class SPlayCommand extends Command {

    private final Lava lava = Lava.lava;
    private AudioLoader manager = AudioLoader.getManagerInstance();
    public static TLongObjectMap<Message> usersFormToReply = new TLongObjectHashMap<>();
    public static TLongObjectMap<TIntObjectMap<AudioTrack>> userChoices = new TLongObjectHashMap<>();

    public SPlayCommand() {
        this.commandName = "splay";
        this.description = "Gives you search results to pick from";
        this.usage = PREFIX + commandName + " [sc] [songname]";
        this.aliases = new String[]{"search", "searchplay", "sp"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT};
        this.id = 45;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        boolean access = Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".*", 1);
        VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            MessageHelper.sendUsage(this, event);
            return;
        }
        StringBuilder sb = new StringBuilder();
        argsToSongName(args, sb, PlayCommand.providers);
        String songName = sb.toString();
        switch (args[0].toLowerCase()) {
            case "sc":
            case "soundcloud":
                if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".sc", 0) || access) {
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    manager.searchTracks(event.getTextChannel(), "scsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                }
                break;
            default:
                if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || access) {
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    manager.searchTracks(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                }
                break;

        }
    }

    static void argsToSongName(String[] args, StringBuilder sb, List<String> providers) {
        if (providers.contains(args[0].toLowerCase())) {
            int i = 0;
            for (String s : args) {
                if (i != 0) sb.append(s).append(" ");
                i++;
            }
        } else {
            for (String s : args) {
                sb.append(s).append(" ");
            }
        }
    }
}
