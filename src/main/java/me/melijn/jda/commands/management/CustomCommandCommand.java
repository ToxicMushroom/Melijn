package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.CommandClientImpl.serverHasCC;

public class CustomCommandCommand extends Command {

    public static final int limitCC = 10;
    private static final int limitAliases = 5;

    public CustomCommandCommand() {
        this.commandName = "customCommand";
        this.description = "Manage custom commands";
        this.usage = PREFIX + commandName + " <add | remove | list | update | info | attachment | prefix | aliases | description> [customCommandName] [message | settings]";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"cc", "cCommand", "customC"};
        this.needs = new Need[]{Need.GUILD};
        this.extra = "https://leovoel.github.io/embed-visualizer/ <- handy link if you want embeds ;)";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 0 || args[0].isBlank()) {
                MessageHelper.sendUsage(this, event);
                return;
            }

            if (args[0].equalsIgnoreCase("add")) {
                if (args.length < 3) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <name> <message>");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                String message = event.getArgs().replaceFirst("add\\s+" + name + "\\s+?", "");
                if (Melijn.mySQL.addCustomCommand(guild.getIdLong(), name, message))
                    event.reply("Custom command **" + name + "** has been added!");
                else event.reply("The command already exists or you have hit the limit of " + limitCC + " commands");
                serverHasCC.put(guild.getIdLong(), true);


            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " remove <name>");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                if (Melijn.mySQL.removeCustomCommand(guild.getIdLong(), name))
                    event.reply("Custom command **" + name + "** has been removed by **" + event.getFullAuthorName() + "**");
                else event.reply("I couldn't find a command named: **" + name + "**\nList: " + getCommandList(guild));
                serverHasCC.invalidate(guild.getIdLong());


            } else if (args[0].equalsIgnoreCase("list")) {
                JSONArray cCommands = Melijn.mySQL.getCustomCommands(guild.getIdLong());
                StringBuilder sb = new StringBuilder("```INI\n");
                for (int i = 0; i < cCommands.length(); i++) {
                    JSONObject obj = (JSONObject) cCommands.get(i);
                    sb.append(i + 1).append(" - [").append(obj.get("name")).append("]\n");
                }
                sb.append("```");
                event.reply(sb.toString());


            } else if (args[0].equalsIgnoreCase("update")) {
                if (args.length < 3) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " update <name> <message>");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                String message = event.getArgs().replaceFirst("update\\s+" + name + "\\s+", "");
                Melijn.mySQL.updateCustomCommand(guild.getIdLong(), name, message);
                event.reply("Custom command **" + name + "** has been updated by **" + event.getFullAuthorName() + "**");


            } else if (args[0].equalsIgnoreCase("info")) {
                if (args.length < 2) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " info <name>");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                JSONObject command = Melijn.mySQL.getCustomCommand(guild.getIdLong(), name);
                if (command == null) {
                    event.reply("I couldn't find a command named: **" + name  + "**\nList: " + getCommandList(guild));
                    return;
                }

                String aliases = command.getString("aliases").isBlank() ? "N/A" : command.getString("aliases");
                String attachment = command.getString("attachment").isBlank() ? "N/A" : "**[link](" + command.getString("attachment") + ")**";
                event.reply(new EmbedBuilder()
                        .setTitle("CustomCommand: " + command.getString("name"))
                        .setColor(Helpers.EmbedColor)
                        .addField("description", command.getString("description"), false)
                        .addField("message", "```JSON\n" + command.getString("message") + "```", false)
                        .addField("aliases", aliases, true)
                        .addField("prefix", command.getBoolean("prefix") ? "enabled" : "disabled", true)
                        .addField("attachment", attachment, true)
                        .build());


            } else if (args[0].equalsIgnoreCase("prefix")) {
                if (args.length < 2) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " prefix <name> [on | off]");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                JSONObject command = Melijn.mySQL.getCustomCommand(guild.getIdLong(), name);
                if (command == null) {
                    event.reply("I couldn't find a command named: **" + name  + "**\nList: " + getCommandList(guild));
                    return;
                }
                if (args.length == 2) {
                    event.reply("The prefix of this command is turned **" + (command.getBoolean("prefix") ? "on**" : "off**"));
                } else if (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off")) {
                    Melijn.mySQL.updateCustomCommandPrefix(guild.getIdLong(), name, args[2].equalsIgnoreCase("on"));
                    event.reply("The prefix for this command has been **" + (command.getBoolean("prefix") ? "enabled" : "disabled")
                            + "** by **" + event.getFullAuthorName() + "**");
                }


            } else if (args[0].equalsIgnoreCase("description")) {
                if (args.length < 2) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " description <name> [text]");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                JSONObject command = Melijn.mySQL.getCustomCommand(guild.getIdLong(), name);
                if (command == null) {
                    event.reply("I couldn't find a command named: **" + name  + "**\nList:" + getCommandList(guild));
                    return;
                }
                if (args.length == 2) {
                    event.reply("The description of this command is **" + command.getString("description") + "**");
                } else {
                    String desc = event.getArgs().replaceFirst("description\\s+" + name + "\\s+", "");
                    if (desc.equalsIgnoreCase("null")) desc = "";
                    Melijn.mySQL.updateCustomCommandDescription(guild.getIdLong(), name, desc);
                    event.reply("The description for this command has been changed to **" + desc + "** by **" + event.getFullAuthorName() + "**");
                }


            } else if (args[0].equalsIgnoreCase("aliases")) {
                if (args.length < 3) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " aliases <name> <add | remove | list> [alias]");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                JSONObject command = Melijn.mySQL.getCustomCommand(guild.getIdLong(), name);
                if (command == null) {
                    event.reply("I couldn't find a command named: **" + name  + "**\nList: " + getCommandList(guild));
                    return;
                }
                List<String> aliases = command.getString("aliases").isBlank() ? new ArrayList<>() : Arrays.asList(command.getString("aliases").split("\\+,\\+"));
                if (args[2].equalsIgnoreCase("list")) {
                    event.reply("Aliases for this command: " + command.getString("aliases"));
                } else if (args[2].equalsIgnoreCase("add") && args.length > 3) {
                    if (aliases.contains(args[3])) {
                        event.reply("This is already an alias of this command\nList: **" + command.getString("aliases") + "**");
                        return;
                    }
                    if (aliases.size() >= limitAliases) {
                        event.reply("You have hit the limit of " + limitAliases + " aliases per command");
                        return;
                    }
                    aliases.add(args[3]);
                    Melijn.mySQL.updateCustomCommandAliases(guild.getIdLong(), name, aliases);
                    event.reply("Successfully added this alias to the command");
                } else if (args[2].equalsIgnoreCase("remove") && args.length > 3) {
                    if (!aliases.contains(args[3])) {
                        event.reply("This is not an alias of this command\nList: **" + command.getString("aliases") + "**");
                        return;
                    }
                    aliases.remove(args[3]);
                    Melijn.mySQL.updateCustomCommandAliases(guild.getIdLong(), name, aliases);
                    event.reply("Successfully removed this alias from the command");
                } else {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " aliases <name> <add | remove | list> [alias]");
                }


            } else if (args[0].equalsIgnoreCase("attachment")) {
                if (args.length < 2) {
                    event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " attachment <name> [file]");
                    return;
                }
                String name = args[1];
                if (name.length() > 128) {
                    event.reply("A CustomCommandName cannot be longer then **128** characters yours was: **" + (name.length() - 128) + "** characters to long");
                    return;
                }
                JSONObject command = Melijn.mySQL.getCustomCommand(guild.getIdLong(), name);
                if (command == null) {
                    event.reply("I couldn't find a command named: **" + name  + "**\nList: " + getCommandList(guild));
                    return;
                }
                if (args.length == 2) {
                    event.reply("The attachment of this command is **" + command.getString("attachment") + "**");
                } else {
                    String url = event.getArgs().replaceFirst("attachment\\s+" + name + "\\s+", "");
                    if (event.getMessage().getAttachments().size() > 0) {
                        url = event.getMessage().getAttachments().get(0).getUrl();
                    }
                    if (url.equalsIgnoreCase("null")) url = "";
                    Melijn.mySQL.updateCustomCommandAttachment(guild.getIdLong(), name, url);
                    event.reply("The attachment for this command has been changed to **" + url + "** by **" + event.getFullAuthorName() + "**");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private String getCommandList(Guild guild) {
        JSONArray cCommands = Melijn.mySQL.getCustomCommands(guild.getIdLong());
        StringBuilder sb = new StringBuilder("```INI\n");
        for (int i = 0; i < cCommands.length(); i++) {
            JSONObject obj = (JSONObject) cCommands.get(i);
            sb.append(i + 1).append(" - [").append(obj.get("name")).append("]\n");
        }
        sb.append("```");
        if (sb.toString().length() < 12) return "";
        return sb.toString();
    }
}
