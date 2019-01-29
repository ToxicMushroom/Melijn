package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import java.util.ArrayList;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class PrivatePrefixCommand extends Command {

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
            event.sendUsage(this, event);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length == 1) {
                    event.reply(event.getVariables().prefixes.getUnchecked(event.getGuild().getIdLong()) + commandName + " add <prefix>");
                    return;
                }
                String prefix = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                List<String> list = new ArrayList<>(event.getVariables().privatePrefixes.getUnchecked(event.getAuthorId()));
                if (list.contains(prefix)) {
                    event.reply("You can't add the same prefix twice");
                    return;
                }
                if (list.size() < 5 && prefix.length() < 21 && !prefix.isEmpty()) {
                    event.getMySQL().addPrivatePrefix(event.getAuthorId(), prefix);
                    list.add(prefix);
                    event.getVariables().privatePrefixes.put(event.getAuthorId(), list);
                    event.reply("Your private prefix '" + prefix + "' has been added");
                } else {
                    event.reply(extra);
                }
                break;
            case "remove":
                if (args.length == 1) {
                    event.reply(event.getVariables().prefixes.getUnchecked(event.getGuild().getIdLong()) + commandName + " add <prefix>");
                    return;
                }
                prefix = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                list = new ArrayList<>(event.getVariables().privatePrefixes.getUnchecked(event.getAuthorId()));
                if (list.contains(prefix)) {
                    event.getMySQL().removePrivatePrefix(event.getAuthorId(), prefix);
                    list.remove(prefix);
                    event.getVariables().privatePrefixes.put(event.getAuthorId(), list);
                    event.reply("Your private prefix '" + prefix + "' has been removed");
                } else {
                    event.reply(extra);
                }
                break;
            case "list":
                list = event.getVariables().privatePrefixes.getUnchecked(event.getAuthorId());
                StringBuilder sb = new StringBuilder("```Markdown\n");
                list.forEach(string -> sb.append(list.indexOf(string) + 1).append(". ").append(string).append("\n"));
                sb.append("```");
                if (list.size() == 0) sb.replace(0, sb.length(), "Looks empty to me");
                event.reply(sb.toString());
                break;
            default:
                event.sendUsage(this, event);
                break;
        }
    }
}
