package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
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
        this.usage = PREFIX + commandName + " [sc] [songname]\nsc only has to be used when you want to search on soundcloud";
        this.aliases = new String[]{"search", "searchplay"};
        this.category = Category.MUSIC;
    }

    private List<String> providers = new ArrayList<>(Arrays.asList("yt", "sc", "link", "youtube", "soundcloud"));
    private MusicManager manager = MusicManager.getManagerinstance();
    public static HashMap<User, Message> usersFormToReply = new HashMap<>();
    public static HashMap<User, HashMap<Integer, AudioTrack>> userChoices = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            boolean acces = Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".*", 1);
            VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
            String args[] = event.getArgs().split("\\s+");
            if (senderVoiceChannel == null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("I'm a pet so i follow you everywhere :3");
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription("PS: you need to join a voice channel then when you use the command then I'll party with you");
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
                return;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {//no args -> usage:
                if (event.getGuild() != null) {
                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                } else {
                    event.reply(usage);
                }
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
                        manager.searchTracks(event.getTextChannel(), "scsearch:" + songname, event.getAuthor());
                    } else {
                        event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                    }
                    break;
                case "yt":
                case "youtube":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.searchTracks(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor());
                    } else {
                        event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                    }
                    break;
                case "link":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.searchTracks(event.getTextChannel(), args[(args.length - 1)], event.getAuthor());
                    } else {
                        event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                    }
                    break;
                default:
                    if (songname.contains("https://") || songname.contains("http://")) {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || acces) {
                            if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                            if (songname.contains("open.spotify.com")) {
                                event.reply("You can't play spotify links with bots sadly :(\nIf you have a self made playlist you can use http://www.playlist-converter.net/#/ to convert it into a youtube playlist or soundcloud (those are supported).");
                                return;
                            }
                            manager.searchTracks(event.getTextChannel(), args[(args.length - 1)], event.getAuthor());
                        } else {
                            event.reply("You need the permission `" + commandName + ".link` to execute this command.");
                        }
                    } else {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                            if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                            manager.searchTracks(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor());

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
