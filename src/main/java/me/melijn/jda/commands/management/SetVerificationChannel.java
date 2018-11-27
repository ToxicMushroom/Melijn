package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationChannel extends Command {

    public static final LoadingCache<Long, Long> verificationChannelsCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.VERIFICATION);
                }
            });

    public SetVerificationChannel() {
        this.commandName = "setVerificationChannel";
        this.usage = PREFIX + commandName + " [TextChannel | null]";
        this.description = "Sets the channel in which the members will have to prove that they are not a bot by entering the VerificationCode";
        this.aliases = new String[]{"svc"};
        this.extra = "You can manually approve users by using the verify command";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            long verificationChannelId = verificationChannelsCache.getUnchecked(guild.getIdLong());
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isBlank()) {
                long id = Helpers.getTextChannelByArgsN(event, args[0]);
                if (id == -1L) {
                    event.reply("Unknown TextChannel");
                } else if (id == 0L) {
                    long oldChannel = verificationChannelsCache.getUnchecked(guild.getIdLong());
                    event.reply("VerificationChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION);
                        verificationChannelsCache.invalidate(guild.getIdLong());
                    });
                } else {
                    if (event.getGuild().getSelfMember().hasPermission(guild.getTextChannelById(id), Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE)) {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.VERIFICATION);
                            verificationChannelsCache.put(guild.getIdLong(), id);
                        });
                        String oldChannel = verificationChannelId == -1 ? "nothing" : "<#" + verificationChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("VerificationChannel has been changed from " + oldChannel + " to " + newChannel);
                    } else {
                        event.reply("I need to be able to attach files and manage messages in <#" + verificationChannelId + "> in order to set it as my VerificationChannel\nYou might also want to give me kick permissions so bot's get kicked after 3 tries/incorrect answers");
                    }
                }
            } else {
                if (verificationChannelId != -1)
                    event.reply("Current VerificationChannel: <#" + verificationChannelId + ">");
                else
                    event.reply("Current VerificationChannel is unset");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
