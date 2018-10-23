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

import static me.melijn.jda.Melijn.PREFIX;

public class SetLeaveMessageCommand extends Command {

    public static final LoadingCache<Long, String> leaveMessages = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public String load(@NotNull Long key) {
                    return Melijn.mySQL.getMessage(key, MessageType.LEAVE);
                }
            });

    public SetLeaveMessageCommand() {
        this.commandName = "setLeaveMessage";
        this.description = "Setup a message that a user get's when he/she/it leaves";
        this.usage = PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders: `%USER%` = user mention // `%USERNAME%` = user name // `%GUILDNAME%` = your discord server's name // `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"slm"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
            Guild guild = event.getGuild();
            String oldMessage = leaveMessages.getUnchecked(guild.getIdLong()).isBlank() ? "nothing" : ("'" + leaveMessages.getUnchecked(guild.getIdLong()) + "'");
            String newMessage = event.getArgs();
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isBlank()) {
                if (args.length == 1 && newMessage.equalsIgnoreCase("null")) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeMessage(guild.getIdLong(), MessageType.LEAVE);
                        leaveMessages.invalidate(guild.getIdLong());
                    });
                    event.reply("LeaveMessage has been changed from " + oldMessage + " to nothing by **" + event.getFullAuthorName() + "**");
                } else {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE);
                        leaveMessages.put(guild.getIdLong(), newMessage);
                    });
                    event.reply("LeaveMessage has been changed from " + oldMessage + " to '" + newMessage + "' by **" + event.getFullAuthorName() + "**");
                }
            } else {
                event.reply("LeaveMessage is set to " + (leaveMessages.getUnchecked(guild.getIdLong()).isBlank() ? "nothing" : "'" + leaveMessages.getUnchecked(guild.getIdLong()) + "'"));
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
