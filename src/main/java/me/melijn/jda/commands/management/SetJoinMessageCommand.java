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

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetJoinMessageCommand extends Command {

    public static final LoadingCache<Long, String> joinMessages = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public String load(Long key) {
                    return Melijn.mySQL.getMessage(key, MessageType.JOIN);
                }
            });

    public SetJoinMessageCommand() {
        this.commandName = "setJoinMessage";
        this.description = "Sets the message that will be sent in the WelcomeChannel when a user joins";
        this.usage = PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders:" +
                " `%USER%` = joined user mention //" +
                " `%USERNAME%` = user name //" +
                " `%GUILDNAME%` = your discord server's name //" +
                " `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"sjm"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 37;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String oldMessage = joinMessages.getUnchecked(guild.getIdLong()).isEmpty() ? "nothing" : ("'" + joinMessages.getUnchecked(guild.getIdLong()) + "'");
            String newMessage = event.getArgs();
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                if (args.length == 1 && args[0].equalsIgnoreCase("null")) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeMessage(guild.getIdLong(), MessageType.JOIN);
                        joinMessages.invalidate(guild.getIdLong());
                    });
                    MessageHelper.sendSplitMessage(event.getTextChannel(), "JoinMessage has been changed from \n" +  oldMessage + "\n to nothing by **" + event.getFullAuthorName() + "**");
                } else {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.JOIN);
                        joinMessages.put(guild.getIdLong(), newMessage);
                    });
                    MessageHelper.sendSplitMessage(event.getTextChannel(),
                            "JoinMessage has been changed from \n" + oldMessage + "\n to \n" + newMessage + "\nby **" + event.getFullAuthorName() + "**");
                }
            } else {
                MessageHelper.sendSplitMessage(event.getTextChannel(), "JoinMessage is set to:\n" + oldMessage);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
