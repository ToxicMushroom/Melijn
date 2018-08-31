package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SetPrefixCommand extends Command {

    public SetPrefixCommand() {
        this.commandName = "setPrefix";
        this.description = "Change the prefix for the commands for your guild";
        this.usage = Melijn.PREFIX + this.commandName + " [prefix]";
        this.aliases = new String[]{"prefix"};
        this.category = Category.MANAGEMENT;
    }

    public static final LoadingCache<Long, String> prefixes = CacheBuilder.newBuilder()
            .maximumSize(200)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public String load(@NotNull Long key) {
                    return Melijn.mySQL.getPrefix(key);
                }
            });

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase(""))
                    event.reply(prefixes.getUnchecked(event.getGuild().getIdLong()));
                else if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                    if (Arrays.toString(args).length() <= 10) {
                        Melijn.MAIN_THREAD.submit(() -> {
                            Melijn.mySQL.setPrefix(guild.getIdLong(), args[0]);
                            event.reply("The prefix has been set to `" + args[0] + "`");
                        });
                    } else {
                        event.reply("The maximum prefix size is 10 characters");
                    }
                } else {
                    event.reply("You need the permission `" + commandName + "` to change the prefix.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
