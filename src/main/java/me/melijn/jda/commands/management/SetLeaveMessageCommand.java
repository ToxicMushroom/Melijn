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
        this.description = "Sets the message that will be sent in the WelcomeChannel when a user leaves";
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
                    MessageHelper.sendSplitMessage(event.getTextChannel(), "LeaveMessage has been changed from \n" + oldMessage + "\n to nothing by **" + event.getFullAuthorName() + "**");
                } else {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE);
                        leaveMessages.put(guild.getIdLong(), newMessage);
                    });
                    MessageHelper.sendSplitMessage(event.getTextChannel(),
                            "LeaveMessage has been changed from \n" + oldMessage + "\n to \n" + newMessage + "\nby **" + event.getFullAuthorName() + "**");
                }
            } else {
                MessageHelper.sendSplitMessage(event.getTextChannel(), "LeaveMessage is set to:\n" + oldMessage);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
