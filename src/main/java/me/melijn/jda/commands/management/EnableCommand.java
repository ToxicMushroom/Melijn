package me.melijn.jda.commands.management;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;

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
                TIntList buffer = new TIntArrayList();
                if (DisableCommand.disabledGuildCommands.containsKey(guild.getIdLong()))
                        buffer.addAll(DisableCommand.disabledGuildCommands.get(guild.getIdLong()));

                for (Command cmd : event.getClient().getCommands()) {
                    if (cmd.getCommandName().equalsIgnoreCase(args[0])) {
                        if (buffer.contains(event.getClient().getCommands().indexOf(cmd))) {
                            buffer.remove(event.getClient().getCommands().indexOf(cmd));
                        } else {
                            event.reply("**" + cmd.getCommandName() + "** was already enabled");
                            return;
                        }
                    }
                    if (cmd.getCategory().toString().equalsIgnoreCase(args[0]) && buffer.contains(event.getClient().getCommands().indexOf(cmd)))
                        buffer.remove(event.getClient().getCommands().indexOf(cmd));
                }
                if (buffer.size() == (DisableCommand.disabledGuildCommands.containsKey(guild.getIdLong()) ? DisableCommand.disabledGuildCommands.get(guild.getIdLong()).size() : 0)) {
                    event.reply("The given command or category was unknown");
                } else {
                    event.reply("Successfully enabled **" + args[0] + "**");
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeDisabledCommands(guild.getIdLong(), buffer);
                        DisableCommand.disabledGuildCommands.put(guild.getIdLong(), buffer);
                    });
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }
}
