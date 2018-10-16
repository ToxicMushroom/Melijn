package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SetJoinLeaveChannelCommand extends Command {

    public static final LoadingCache<Long, Long> welcomeChannelCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, ChannelType.WELCOME);
                }
            });

    public SetJoinLeaveChannelCommand() {
        this.commandName = "setJoinLeaveChannel";
        this.description = "Setup a TextChannel where users will be welcomed or leave";
        this.usage = Melijn.PREFIX + commandName + " <TextChannel | null>";
        this.aliases = new String[]{"sjlc", "SetWelcomeChannel", "swc"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                long welcomeChannelId = welcomeChannelCache.getUnchecked(guild.getIdLong());
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].isBlank()) {
                    long id = Helpers.getTextChannelByArgsN(event, args[0]);
                    if (id == -1L) {
                        event.reply("Unknown TextChannel");
                    } else if (id == 0L) {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.WELCOME);
                            welcomeChannelCache.invalidate(guild.getIdLong());
                        });
                        long oldChannel = welcomeChannelCache.getUnchecked(guild.getIdLong());
                        event.reply("WelcomeChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    } else {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WELCOME);
                            welcomeChannelCache.put(guild.getIdLong(), id);
                            if (SetJoinMessageCommand.joinMessages.getUnchecked(guild.getIdLong()).isBlank()) {
                                Melijn.mySQL.setMessage(guild.getIdLong(), "Welcome **%USER%** to our awesome discord server :D", MessageType.JOIN);
                                SetJoinMessageCommand.joinMessages.put(guild.getIdLong(), "Welcome %USER% to the %GUILDNAME% discord server");
                            }
                            if (SetLeaveMessageCommand.leaveMessages.getUnchecked(guild.getIdLong()).isBlank()) {
                                Melijn.mySQL.setMessage(guild.getIdLong(), "**%USERNAME%** left us :C", MessageType.LEAVE);
                                SetLeaveMessageCommand.leaveMessages.put(guild.getIdLong(), "**%USERNAME%** left us :C");
                            }
                            event.reply("I've set the default join and leave message :beginner:");

                            String oldChannel = welcomeChannelId == -1 ? "nothing" : "<#" + welcomeChannelId + ">";
                            String newChannel = "<#" + id + ">";
                            event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
                        });
                    }
                } else {
                    if (welcomeChannelId != -1)
                        event.reply("Current WelcomeChannel: <#" + welcomeChannelId + ">");
                    else
                        event.reply("Current WelcomeChannel is unset");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }


}
