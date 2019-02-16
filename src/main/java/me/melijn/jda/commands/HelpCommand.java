package me.melijn.jda.commands;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class HelpCommand extends Command {

    public HelpCommand() {
        this.commandName = "help";
        this.description = "Shows you help for commands";
        this.usage = PREFIX + commandName + " [command]";
        this.aliases = new String[]{"commands", "cmds", "cmd"};
        this.category = Category.DEFAULT;
        this.id = 56;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0) {
            for (Command command : event.getClient().getCommands()) {
                List<String> aliases = new ArrayList<>(Arrays.asList(command.getAliases()));
                if (command.getCommandName().equalsIgnoreCase(args[0]) || aliases.contains(args[0])) {
                    event.reply("**Help off " + command.getCommandName() +
                            "**\n**Usage:**  `" + command.getUsage() +
                            "`\n**Description:**  " + command.getDescription() +
                            (command.getExtra().isEmpty() ? "" : "\n**Extra:**  " + command.getExtra()));
                    return;
                }
            }
        }
        if (event.getGuild() != null) {
            event.reply("https://melijn.com/server/" + event.getGuild().getId() + "/commands");
        } else {
            event.reply("https://melijn.com/commands");
        }
    }
}
