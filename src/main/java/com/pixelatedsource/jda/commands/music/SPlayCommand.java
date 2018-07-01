package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SPlayCommand extends Command {

    public SPlayCommand() {
        this.commandName = "splay";
        this.description = "Gives you the search results to pick from instead of playing the first song of the results (that's what >play does)";
        this.usage = PREFIX + commandName + " [sc] [songname]";
        this.aliases = new String[]{"search", "searchplay", "sp"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT};
    }

    private List<String> providers = new ArrayList<>(Arrays.asList("yt", "sc", "link", "youtube", "soundcloud"));
    private MusicManager manager = MusicManager.getManagerinstance();
    public static HashMap<User, Message> usersFormToReply = new HashMap<>();
    public static HashMap<User, HashMap<Integer, AudioTrack>> userChoices = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        boolean access = Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".*", 1);
        VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String args[] = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].equalsIgnoreCase("")) {
            MessageHelper.sendUsage(this, event);
            return;
        }
        StringBuilder sb = new StringBuilder();
        argsToSongName(args, sb, providers);
        String songName = sb.toString();
        switch (args[0].toLowerCase()) {
            case "sc":
            case "soundcloud":
                if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".sc", 0) || access) {
                    if (isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                    manager.searchTracks(event.getTextChannel(), "scsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                }
                break;
            default:
                if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || access) {
                    if (isConnectedOrConnecting(event, guild, senderVoiceChannel)) return;
                    manager.searchTracks(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                }
                break;

        }
    }

    static boolean isConnectedOrConnecting(CommandEvent event, Guild guild, VoiceChannel senderVoiceChannel) {
        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) {
            if (senderVoiceChannel.getUserLimit() == 0 || (senderVoiceChannel.getUserLimit() > senderVoiceChannel.getMembers().size() || guild.getSelfMember().hasPermission(senderVoiceChannel, Permission.VOICE_MOVE_OTHERS))) {
                guild.getAudioManager().openAudioConnection(senderVoiceChannel);
            } else {
                event.reply("Your channel is full. I need the **Move Members** permission to join full channels");
                return true;
            }
        }
        return false;
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
