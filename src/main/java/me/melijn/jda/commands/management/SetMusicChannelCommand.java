package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SetMusicChannelCommand extends Command {

    public SetMusicChannelCommand() {
        this.commandName = "setMusicChannel";
        this.description = "Set the music channel to a channel so the bot wil auto join ect";
        this.usage = Melijn.PREFIX + commandName + " [VoiceChannel | null]";
        this.aliases = new String[]{"smc"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
    }

    public static final LoadingCache<Long, Long> musicChannelCache = CacheBuilder.newBuilder()
            .maximumSize(15)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.MUSIC);
                }
            });

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                event.reply(musicChannelCache.getUnchecked(guild.getIdLong()) == -1 ? "The MusicChannel is unset" : "MusicChannel: <#" + musicChannelCache.getUnchecked(guild.getIdLong()) + ">");
            } else {
                long channelId = Helpers.getVoiceChannelByArgsN(event, args[0]);
                if (channelId == -1) {
                    MessageHelper.sendUsage(this, event);
                } else if (channelId == 0) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC);
                        musicChannelCache.invalidate(guild.getIdLong());
                    });
                    event.reply("The MusicChannel has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setChannel(guild.getIdLong(), channelId, ChannelType.MUSIC);
                        musicChannelCache.put(guild.getIdLong(), channelId);
                    });
                    event.reply("The MusicChannel has been set to <#" + args[0] + "> by **" + event.getFullAuthorName() + "**");
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
