package me.melijn.jda.commands.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.commands.management.SetPrefixCommand;
import me.melijn.jda.utils.MessageHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class PrivatePrefixCommand extends Command {

    public static final LoadingCache<Long, List<String>> privatePrefixes = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public List<String> load(@NotNull Long key) {
                    return Melijn.mySQL.getPrivatePrefixes(key);
                }
            });

    public PrivatePrefixCommand() {
        this.commandName = "privatePrefix";
        this.usage = PREFIX + commandName + " <add | remove | list> [prefix]";
        this.description = "Main command to manage your private prefixes";
        this.category = Category.UTILS;
        this.aliases = new String[]{"pp", "privatep", "pprefix"};
        this.extra = "The prefix can not be longer then 20 characters and you can have a maximum of 5 prefixes";
        this.id = 107;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0) {
            MessageHelper.sendUsage(this, event);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length == 1) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()) + commandName + " add <prefix>");
                    return;
                }
                String prefix = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                List<String> list = new ArrayList<>(privatePrefixes.getUnchecked(event.getAuthorId()));
                if (list.contains(prefix)) {
                    event.reply("You can't add the same prefix twice");
                    return;
                }
                if (list.size() < 5 && prefix.length() < 21 && !prefix.isBlank()) {
                    Melijn.mySQL.addPrivatePrefix(event.getAuthorId(), prefix);
                    list.add(prefix);
                    privatePrefixes.put(event.getAuthorId(), list);
                    event.reply("Your private prefix '" + prefix + "' has been added");
                } else {
                    event.reply(extra);
                }
                break;
            case "remove":
                if (args.length == 1) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()) + commandName + " add <prefix>");
                    return;
                }
                prefix = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                list = new ArrayList<>(privatePrefixes.getUnchecked(event.getAuthorId()));
                if (list.contains(prefix)) {
                    Melijn.mySQL.removePrivatePrefix(event.getAuthorId(), prefix);
                    list.remove(prefix);
                    privatePrefixes.put(event.getAuthorId(), list);
                    event.reply("Your private prefix '" + prefix + "' has been removed");
                } else {
                    event.reply(extra);
                }
                break;
            case "list":
                list = privatePrefixes.getUnchecked(event.getAuthorId());
                StringBuilder sb = new StringBuilder("```Markdown\n");
                list.forEach(string -> sb.append(list.indexOf(string) + 1).append(". ").append(string).append("\n"));
                sb.append("```");
                if (list.size() == 0) sb.replace(0, sb.toString().length(), "Looks empty to me");
                event.reply(sb.toString());
                break;
            default:
                MessageHelper.sendUsage(this, event);
                break;
        }
    }
}
