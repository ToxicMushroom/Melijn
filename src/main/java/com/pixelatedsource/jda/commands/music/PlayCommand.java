package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PlayCommand extends Command {

    public PlayCommand() {
        this.commandName = "play";
        this.description = "plays a song or adds it to the queue";
        this.usage = PREFIX + this.commandName + " [sc] <songname | link>";
        this.extra = "You only have to use sc if you want to search on soundcloud";
        this.aliases = new String[]{"p"};
        this.category = Category.MUSIC;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    private List<String> providers = new ArrayList<>(Arrays.asList("yt", "sc", "link", "youtube", "soundcloud"));
    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            boolean acces = Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".*", 1);
            VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
            String args[] = event.getArgs().split("\\s+");
            if (senderVoiceChannel == null && !guild.getSelfMember().getVoiceState().inVoiceChannel()) {
                event.reply("Please join a VoiceChannel");
                return;
            }
            if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel() && event.getGuild().getSelfMember().getVoiceState().getChannel() != senderVoiceChannel) {
                event.reply("You have to be in the same VoiceChannel as me to add music");
                return;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {//no args -> usage:
                MessageHelper.sendUsage(this, event);
                return;
            }
            if (!event.getGuild().getSelfMember().hasPermission(senderVoiceChannel, Permission.VOICE_CONNECT)) {
                event.reply("I don't have the permission VOICE_CONNECT");
                return;
            }
            String songname;
            StringBuilder sb = new StringBuilder();
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
            songname = sb.toString();
            switch (args[0].toLowerCase()) {
                case "sc":
                case "soundcloud":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".sc", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.loadTrack(event.getTextChannel(), "scsearch:" + songname, event.getAuthor(), false);
                    } else {
                        event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                    }
                    break;
                case "yt":
                case "youtube":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor(), false);
                    } else {
                        event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                    }
                    break;
                case "link":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                    } else {
                        event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                    }
                    break;
                default:
                    if (songname.contains("https://") || songname.contains("http://")) {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || acces) {
                            if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect())
                                guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                            if (songname.contains("open.spotify.com")) {
                                event.reply("You can't play spotify links with bots rn sadly :(\nIf you have a self made playlist you can use http://www.playlist-converter.net/#/ to convert it into a youtube/soundcloud playlist");
                                return;
                            }
                            manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                        } else {
                            event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                        }
                    } else {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                            if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                            manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor(), false);
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