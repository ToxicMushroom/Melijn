package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.commands.HelpCommand;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;

import static me.melijn.jda.Melijn.PREFIX;

public class EnableCommand extends Command {

    public EnableCommand() {
        this.commandName = "enable";
        this.description = "Fully disables a command from being used";
        this.usage = PREFIX + commandName + " <commandName | category>";
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                ArrayList<Integer> buffer = new ArrayList<>(DisableCommand.disabledGuildCommands.getOrDefault(guild.getIdLong(), new ArrayList<>()));

                for (Command cmd : HelpCommand.commandList) {
                    if (cmd.getCommandName().equalsIgnoreCase(args[0])) {
                        if (buffer.contains(HelpCommand.commandList.indexOf(cmd))) {
                            buffer.remove(buffer.indexOf(HelpCommand.commandList.indexOf(cmd)));
                        } else {
                            event.reply("**" + cmd.getCommandName() + "** was already enabled");
                            return;
                        }
                    }
                    if (cmd.getCategory().toString().equalsIgnoreCase(args[0]))
                        if (buffer.contains(HelpCommand.commandList.indexOf(cmd)))
                            buffer.remove(buffer.indexOf(HelpCommand.commandList.indexOf(cmd)));
                }
                if (buffer.size() == DisableCommand.disabledGuildCommands.getOrDefault(guild.getIdLong(), new ArrayList<>()).size()) {
                    event.reply("The given command or category was unknown");
                } else {
                    event.reply("Successfully enabled **" + args[0] + "**");
                    Melijn.MAIN_THREAD.submit(() -> {
                        Melijn.mySQL.removeDisabledCommands(guild.getIdLong(), buffer);
                        if (DisableCommand.disabledGuildCommands.replace(guild.getIdLong(), buffer) == null)
                            DisableCommand.disabledGuildCommands.put(guild.getIdLong(), buffer);
                    });
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }
}
