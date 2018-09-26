package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

public class FilterCommand extends Command {

    private MySQL mySQL = Melijn.mySQL;

    public FilterCommand() {
        this.commandName = "filter";
        this.description = "The bot will remove messages which contain prohibited words";
        this.usage = Melijn.PREFIX + commandName + " <allowed | denied> <add | remove | list> [word]";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length >= 2) {
                    String content = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1], "").replaceFirst("\\s+", "");
                    switch (args[0]) {
                        case "allowed":
                            switch (args[1]) {
                                case "add":
                                    if (args.length > 2) {
                                        mySQL.addFilter(guild.getIdLong(), "allowed", content);
                                        event.reply("`" + content + "` has been added to the allowed list.");
                                    } else {
                                        MessageHelper.sendUsage(this, event);
                                    }
                                    break;
                                case "remove":
                                    if (args.length > 2) {
                                        mySQL.removeFilter(guild.getIdLong(), "allowed", content);
                                        event.reply("`" + content + "` has been removed from the allowed list.");
                                    } else {
                                        MessageHelper.sendUsage(this, event);
                                    }
                                    break;
                                case "list":
                                    int filterNumber = 0;
                                    StringBuilder partBuilder = new StringBuilder();
                                    partBuilder.append("**Allowed List**\n```Markdown\n");
                                    for (String s : mySQL.getFilters(guild.getIdLong(), "allowed")) {
                                        partBuilder = addListParts(event, partBuilder, s);
                                        partBuilder.append(++filterNumber).append(". ").append(s.replaceAll("`", "´")).append("\n");
                                    }
                                    partBuilder.append("```");
                                    event.reply(partBuilder.toString());
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    break;
                            }
                            break;
                        case "denied":
                            switch (args[1]) {
                                case "add":
                                    if (args.length > 2) {
                                        mySQL.addFilter(guild.getIdLong(), "denied", content);
                                        event.reply("`" + content + "` has been added to the denied list.");
                                    } else {
                                        MessageHelper.sendUsage(this, event);
                                    }
                                    break;
                                case "remove":
                                    if (args.length > 2) {
                                        mySQL.removeFilter(guild.getIdLong(), "denied", content);
                                        event.reply("`" + content + "` has been removed from the denied list.");
                                    } else {
                                        MessageHelper.sendUsage(this, event);
                                    }
                                    break;
                                case "list":
                                    int filterNumber = 0;
                                    StringBuilder partBuilder = new StringBuilder();
                                    partBuilder.append("**Denied List**\n```Markdown\n");
                                    for (String s : mySQL.getFilters(guild.getIdLong(), "denied")) {
                                        partBuilder = addListParts(event, partBuilder, s);
                                        partBuilder.append(++filterNumber).append(". ").append(s.replaceAll("`", "´")).append("\n");
                                    }
                                    partBuilder.append("```");
                                    event.reply(partBuilder.toString());
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    break;
                            }
                            break;
                        default:
                            MessageHelper.sendUsage(this, event);
                            break;
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }

    @NotNull
    private StringBuilder addListParts(CommandEvent event, StringBuilder partBuilder, String s) {
        StringBuilder stringBuilder = partBuilder;
        if (stringBuilder.length() + s.length() > 1900) {
            stringBuilder.append("```");
            event.reply(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("```Markdown\n");
        }
        return stringBuilder;
    }
}
