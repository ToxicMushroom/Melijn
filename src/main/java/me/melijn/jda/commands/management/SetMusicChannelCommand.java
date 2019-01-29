package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.Task;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetMusicChannelCommand extends Command {

    public SetMusicChannelCommand() {
        this.commandName = "setMusicChannel";
        this.description = "Sets the MusicChannel";
        this.usage = PREFIX + commandName + " [VoiceChannel | null]";
        this.aliases = new String[]{"smc"};
        this.needs = new Need[]{Need.GUILD};
        this.extra = "https://melijn.com/guides/guide-5/";
        this.category = Category.MANAGEMENT;
        this.id = 79;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                event.reply(event.getVariables().musicChannelCache.getUnchecked(guild.getIdLong()) == -1 ? "The MusicChannel is unset" : "MusicChannel: <#" + event.getVariables().musicChannelCache.getUnchecked(guild.getIdLong()) + ">");
            } else {
                long channelId = event.getHelpers().getVoiceChannelByArgsN(event, args[0]);
                if (channelId == -1) {
                    event.sendUsage(this, event);
                } else if (channelId == 0) {
                    event.async(() -> {
                        event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.MUSIC);
                        event.getVariables().musicChannelCache.invalidate(guild.getIdLong());
                    });
                    event.reply("The MusicChannel has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    event.async(() -> {
                        event.getMySQL().setChannel(guild.getIdLong(), channelId, ChannelType.MUSIC);
                        event.getVariables().musicChannelCache.put(guild.getIdLong(), channelId);
                    });
                    event.reply("The MusicChannel has been set to <#" + args[0] + "> by **" + event.getFullAuthorName() + "**");
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
