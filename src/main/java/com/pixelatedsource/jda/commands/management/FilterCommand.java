package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class FilterCommand extends Command {

    public FilterCommand() {
        this.commandName = "filter";
        this.description = "The bot will remove messages which contain prohibited words";
        this.usage = PREFIX + commandName + " <allowed | denied> <add | remove | list> [word]";
        this.category = Category.MANAGEMENT;
    }

    MySQL mySQL = PixelSniper.mySQL;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length >= 2) {
                    String content = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "\\s+", "");
                    switch (args[0]) {
                        case "allowed":
                            switch (args[1]) {
                                case "add":
                                    mySQL.addFilter(guild, "allowed", content);
                                    event.reply("`" + content + "` has been added to the allowed list.");
                                    break;
                                case "remove":
                                    mySQL.removeFilter(guild, "allowed", content);
                                    event.reply("`" + content + "` has been removed from the allowed list.");
                                    break;
                                case "list":
                                    int filterNumber = 0;
                                    StringBuilder partBuilder = new StringBuilder();
                                    partBuilder.append("**Allowed List**\n```Markdown\n");
                                    for (String s : mySQL.getFilters(guild, "allowed")) {
                                        if (partBuilder.toString().length() + s.length() > 1900) {
                                            partBuilder.append("```");
                                            event.reply(partBuilder.toString());
                                            partBuilder = new StringBuilder();
                                            partBuilder.append("```Markdown\n");
                                        }
                                        partBuilder.append(++filterNumber).append(". ").append(s.replaceAll("`", "´")).append("\n");
                                    }
                                    partBuilder.append("```");
                                    event.reply(partBuilder.toString());
                                    break;
                                default:
                                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(guild)));
                                    break;
                            }
                            break;
                        case "denied":
                            switch (args[1]) {
                                case "add":
                                    mySQL.addFilter(guild, "denied", content);
                                    event.reply("`" + content + "` has been added to the denied list.");
                                    break;
                                case "remove":
                                    mySQL.removeFilter(guild, "denied", content);
                                    event.reply("`" + content + "` has been removed from the denied list.");
                                    break;
                                case "list":
                                    int filterNumber = 0;
                                    StringBuilder partBuilder = new StringBuilder();
                                    partBuilder.append("**Denied List**\n```Markdown\n");
                                    for (String s : mySQL.getFilters(guild, "denied")) {
                                        if (partBuilder.toString().length() + s.length() > 1900) {
                                            partBuilder.append("```");
                                            event.reply(partBuilder.toString());
                                            partBuilder = new StringBuilder();
                                            partBuilder.append("```Markdown\n");
                                        }
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
}
