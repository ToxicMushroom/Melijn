package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
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
        this.usage = PREFIX + this.commandName + " [sc] <songname | link>\nYou only have to use sc if you want to search on soundcloud";
        this.aliases = new String[]{"p"};
        this.category = Category.MUSIC;
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
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Some info for new people");
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(PixelSniper.mySQL.getPrefix(guild.getId()) + this.commandName + " [yt|sc|link] <Songname>");
                eb.addField("Legenda", "[] = optional" + "| = or" + "<> = needed", true);
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
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
                    }
                    break;
                case "yt":
                case "youtube":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor(), false);
                    }
                    break;
                case "link":
                    if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".link", 0) || acces) {
                        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                        manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
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
                            manager.loadTrack(event.getTextChannel(), args[(args.length - 1)], event.getAuthor(), true);
                        }
                    } else {
                        if (Helpers.hasPerm(guild.getMember(event.getAuthor()), this.commandName + ".yt", 0) || acces) {
                            if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) guild.getAudioManager().openAudioConnection(senderVoiceChannel);
                            manager.loadTrack(event.getTextChannel(), "ytsearch:" + songname, event.getAuthor(), false);
                        }
                    }
                    break;
            }
        }
    }
}