package me.melijn.jda.commands;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;

import java.util.List;
import java.util.stream.Collectors;

import static me.melijn.jda.Melijn.PREFIX;

public class HelpCommand extends Command {

    public HelpCommand() {
        this.commandName = "help";
        this.description = "Shows you help for commands";
        this.usage = PREFIX + commandName + " [list | command]";
        this.aliases = new String[]{"commands", "cmds", "cmd"};
        this.category = Category.DEFAULT;
        this.id = 56;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (event.getArgs().isEmpty()) {
            if (event.getGuild() == null) {
                event.reply("https://melijn.com/commands or >help list");
            } else {
                event.reply("https://melijn.com/server/" + event.getGuildId() + "/commands or " + event.getVariables().prefixes.get(event.getGuildId()) + "help list");
            }
        } else {
            if (args[0].equalsIgnoreCase("list")) {
                List<Command> commandList = event.getClient().getCommands();

                Embedder eb = new Embedder(event.getVariables(), event.getGuild());
                eb.setTitle("Command List", "https://melijn.com/commands");

                String DEFAULT = commandList.stream()
                        .filter(command -> command.getCategory() == Category.DEFAULT)
                        .map(Command::getCommandName)
                        .collect(Collectors.joining("\n"));

                String MUSIC = commandList.stream().
                        filter(command -> command.getCategory() == Category.MUSIC)
                        .map(Command::getCommandName)
                        .collect(Collectors.joining("\n"));

                String UTIL = commandList.stream()
                        .filter(command -> command.getCategory() == Category.UTILS)
                        .map(Command::getCommandName)
                        .collect(Collectors.joining("\n"));

                String MANAGEMENT = commandList.stream()
                        .filter(command -> command.getCategory() == Category.MANAGEMENT)
                        .map(Command::getCommandName)
                        .collect(Collectors.joining("\n"));

                String FUN = commandList.stream()
                        .filter(command -> command.getCategory() == Category.FUN)
                        .map(Command::getCommandName)
                        .collect(Collectors.joining("\n"));

                //Default commands
                eb.addField("Default", DEFAULT, true);

                //Music commands
                eb.addField("Music", MUSIC, true);

                //Util commands
                eb.addField("Utilities", UTIL, true);

                //Management commands
                eb.addField("Management", MANAGEMENT, true);

                //Fun commands
                eb.addField("Fun", FUN, true);

                eb.setFooter("Total command: " + commandList.size(), null);

                event.reply(eb.build());
                return;
            }

            for (Command command : event.getClient().getCommands()) {
                if (command.isCommandFor(args[0])) {
                    event.reply("" +
                            "Help off **" + command.getCommandName() + "**\n" +
                            "**Usage:**  `" + command.getUsage() + "`\n" +
                            (command.getAliases().length > 0 ? ("**Aliases:** `" + String.join(", ", command.getAliases()) + "`\n") : "") +
                            "**Description:**  " + command.getDescription() +
                            (command.getExtra().isEmpty() ? "" : "\n**Extra:**  " + command.getExtra()));
                    return;
                }
            }
        }

    }
}
