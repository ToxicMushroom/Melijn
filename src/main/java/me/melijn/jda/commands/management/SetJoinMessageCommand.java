package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.MessageType;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;

import java.util.concurrent.TimeUnit;

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
        this.description = "Setup a message that a user get's when he/she/it joins";
        this.usage = Melijn.PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders:" +
                " `%USER%` = joined user mention //" +
                " `%USERNAME%` = user name //" +
                " `%GUILDNAME%` = your discord server's name //" +
                " `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"sjm"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String oldMessage = joinMessages.getUnchecked(guild.getIdLong()).isBlank() ? "nothing" : ("'" + joinMessages.getUnchecked(guild.getIdLong()) + "'");
                String newMessage = event.getArgs();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].isBlank()) {
                    if (args.length == 1 && args[0].equalsIgnoreCase("null")) {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.removeMessage(guild.getIdLong(), MessageType.JOIN);
                            joinMessages.invalidate(guild.getIdLong());
                        });
                        event.reply("JoinMessage has been set to nothing by **" + event.getFullAuthorName() + "**");
                    } else {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.JOIN);
                            joinMessages.put(guild.getIdLong(), newMessage);
                        });
                        event.reply("JoinMessage has been changed from " + oldMessage + " to '" + newMessage + "' by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    event.reply("JoinMessage is set to " + oldMessage);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
