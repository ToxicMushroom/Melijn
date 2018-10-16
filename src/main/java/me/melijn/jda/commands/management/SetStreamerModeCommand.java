package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SetStreamerModeCommand extends Command {

    public static final LoadingCache<Long, Boolean> streamerModeCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Boolean load(@NotNull Long key) {
                    return Melijn.mySQL.getStreamerMode(key);
                }
            });

    public SetStreamerModeCommand() {
        this.commandName = "setStreamerMode";
        this.description = "A special mode that lets the bot play a stream in the music channel";
        this.usage = Melijn.PREFIX + commandName + " [true/on | false/off]";
        this.aliases = new String[]{"ssm"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (SetMusicChannelCommand.musicChannelCache.getUnchecked(guild.getIdLong()) == -1) {
                    event.reply("You first have to set a MusicChannel.\n" + SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + "smc <channelId>");
                    return;
                }
                VoiceChannel musicChannel = guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guild.getIdLong()));
                if (musicChannel != null) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length == 0 || args[0].isBlank()) {
                        String state = streamerModeCache.getUnchecked(guild.getIdLong()) ? "enabled" : "disabled";
                        event.reply("StreamerMode: **" + state + "**");
                    } else if (args.length == 1) {
                        switch (args[0]) {
                            case "true":
                            case "on":
                            case "enabled":
                                if (guild.getSelfMember().hasPermission(musicChannel, Permission.VOICE_CONNECT)) {
                                    if (event.getMember().getVoiceState().inVoiceChannel()) {
                                        guild.getAudioManager().openAudioConnection(musicChannel);
                                    }
                                    TaskScheduler.async(() -> {
                                        Melijn.mySQL.setStreamerMode(guild.getIdLong(), true);
                                        streamerModeCache.put(guild.getIdLong(), true);
                                    });
                                    event.reply("\uD83D\uDCF6 The StreamerMode has been **enabled** by **" + event.getFullAuthorName() + "**");
                                } else {
                                    event.reply(String.format("I have no permission to connect to the configured MusicChannel: %s", musicChannel.getName()));
                                }
                                break;
                            case "false":
                            case "off":
                            case "disabled":
                                TaskScheduler.async(() -> {
                                    Melijn.mySQL.setStreamerMode(guild.getIdLong(), false);
                                    streamerModeCache.put(guild.getIdLong(), false);
                                });
                                event.reply("The streamer mode has been **disabled** by **" + event.getFullAuthorName() + "**");
                                break;
                            default:
                                MessageHelper.sendUsage(this, event);
                                break;
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                } else {
                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC);
                    SetMusicChannelCommand.musicChannelCache.invalidate(guild.getIdLong());
                    event.reply("You have to set a MusicChannel to enable this mode :p");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
