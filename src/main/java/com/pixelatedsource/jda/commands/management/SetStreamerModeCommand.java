package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetStreamerModeCommand extends Command {

    public SetStreamerModeCommand() {
        this.commandName = "setstreamermode";
        this.description = "A special mode that lets the bot play a stream in the music channel";
        this.usage = PREFIX + commandName + " [true/on | false/off]";
        this.aliases = new String[] {"ssm"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (PixelSniper.mySQL.getChannelId(guild.getId(), ChannelType.MUSIC) == null) {
                    event.reply("You first have to set a music channel.\n" + PixelSniper.mySQL.getPrefix(event.getGuild().getId()) + "smc <channelId>");
                }
                VoiceChannel musicChannel = guild.getVoiceChannelById(PixelSniper.mySQL.getChannelId(guild.getId(), ChannelType.MUSIC));
                if (musicChannel != null) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                        event.reply("state: " + PixelSniper.mySQL.getStreamerMode(guild.getId()));
                    } else if (args.length == 1) {
                        switch (args[0]) {
                            case "true":
                            case "on":
                                if (event.getMember().hasPermission(musicChannel, Permission.VOICE_CONNECT)) {
                                    if (event.getMember().getVoiceState().inVoiceChannel()) {
                                        guild.getAudioManager().openAudioConnection(musicChannel);
                                    }
                                    PixelSniper.mySQL.setStreamerMode(guild.getId(), true);
                                    event.reply("\uD83D\uDCF6 The streamer mode has been **enabled**.");
                                } else {
                                    event.reply("The bot has no permission to the music channel.");
                                }
                                break;
                            case "false":
                            case "off":
                                PixelSniper.mySQL.setStreamerMode(guild.getId(), false);
                                event.reply("The streamer mode has been **disabled**.");
                                break;
                        }
                    } else {
                        if (event.getGuild() != null) {
                            event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                        } else {
                            event.reply(usage);
                        }
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
