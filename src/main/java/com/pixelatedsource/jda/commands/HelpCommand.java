package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HelpCommand extends Command {

    public HelpCommand() {
        this.commandName = "help";
        this.description = "Gives you the link to this page.";
        this.usage = PREFIX + commandName + " [command]";
        this.aliases = new String[]{"commands", "cmds"};
        this.category = Category.DEFAULT;
    }

    public static ArrayList<Command> commandList = new ArrayList<>();

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0) {
            for (Command command : commandList) {
                List<String> aliases = new ArrayList<>(Arrays.asList(command.getAliases()));
                if (command.getCommandName().equalsIgnoreCase(args[0]) || aliases.contains(args[0])) {
                    event.reply("**Help off " + command.getCommandName() +
                            "**\n**Usage:**  `" + command.getUsage() +
                            "`\n**Description:**  " + command.getDescription() +
                            (command.getExtra().equalsIgnoreCase("") ? "" : "\n**Extra:**  " + command.getExtra()));
                    return;
                }
            }
        }
        if (event.getGuild() != null) {
            event.reply("https://melijn.com/commands/index.php?id=" + event.getGuild().getId());
        } else {
            event.reply("https://melijn.com/commands/");
        }
    }
}
