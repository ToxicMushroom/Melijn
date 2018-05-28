package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetStreamerModeCommand extends Command {

    public SetStreamerModeCommand() {
        this.commandName = "setstreamermode";
        this.description = "A special mode that lets the bot play a stream in the music channel";
        this.usage = PREFIX + commandName + " [true/on | false/off]";
        this.aliases = new String[]{"ssm"};
        this.category = Category.MANAGEMENT;
    }

    public static List<Long> streamerModes = PixelSniper.mySQL.getStreamerModeList();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (!SetMusicChannelCommand.musicChannelIds.containsKey(guild.getIdLong())) {
                    event.reply("You first have to set a MusicChannel.\n" + SetPrefixCommand.prefixes.getOrDefault(guild.getIdLong(), ">") + "smc <channelId>");
                    return;
                }
                VoiceChannel musicChannel = guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelIds.get(guild.getIdLong()));
                if (musicChannel != null) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                        String state = streamerModes.contains(guild.getIdLong()) ? "enabled" : "disabled";
                        event.reply("StreamerMode: **" + state + "**");
                    } else if (args.length == 1) {
                        switch (args[0]) {
                            case "true":
                            case "on":
                            case "enabled":
                                if (event.getMember().hasPermission(musicChannel, Permission.VOICE_CONNECT)) {
                                    if (event.getMember().getVoiceState().inVoiceChannel()) {
                                        guild.getAudioManager().openAudioConnection(musicChannel);
                                    }
                                    new Thread(() -> {
                                        PixelSniper.mySQL.setStreamerMode(guild.getIdLong(), true);
                                        if (!streamerModes.contains(guild.getIdLong())) streamerModes.add(guild.getIdLong());
                                    }).start();
                                    event.reply("\uD83D\uDCF6 The StreamerMode has been **enabled** by **" + event.getFullAuthorName() + "**");
                                } else {
                                    event.reply("The bot has no permission to connect to the MusicChannel.");
                                }
                                break;
                            case "false":
                            case "off":
                            case "disabled":
                                new Thread(() -> {
                                    PixelSniper.mySQL.setStreamerMode(guild.getIdLong(), false);
                                    streamerModes.remove(guild.getIdLong());
                                }).start();
                                event.reply("The streamer mode has been **disabled** by **" + event.getFullAuthorName() + "**");
                                break;
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                } else {
                    event.reply("You have to have set a music channel to enable this mode!");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
