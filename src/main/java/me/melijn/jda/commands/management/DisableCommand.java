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
import java.util.HashMap;

import static me.melijn.jda.Melijn.PREFIX;

public class DisableCommand extends Command {

    public DisableCommand() {
        this.commandName = "disable";
        this.description = "Fully disables a command from being used";
        this.usage = PREFIX + commandName + " <commandName | category>";
        this.needs = new Need[]{Need.GUILD};
        this.aliases = new String[]{"disabled"};
        this.extra = "You can use >disabled to get a list";
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, ArrayList<Integer>> disabledGuildCommands = Melijn.mySQL.getDisabledCommandsMap();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (event.getExecutor().equalsIgnoreCase("disable") && args.length > 0 && !args[0].equalsIgnoreCase("")) {
                ArrayList<Integer> buffer = new ArrayList<>(disabledGuildCommands.getOrDefault(guild.getIdLong(), new ArrayList<>()));
                for (Command cmd : HelpCommand.commandList) {
                    if (cmd.getCommandName().equalsIgnoreCase(args[0]))
                        if (!buffer.contains(HelpCommand.commandList.indexOf(cmd)) && !cmd.getCommandName().equalsIgnoreCase("enable"))
                            buffer.add(HelpCommand.commandList.indexOf(cmd));
                        else {
                            event.reply("**" + cmd.getCommandName() + "** was already disabled");
                            return;
                        }
                    if (cmd.getCategory().toString().equalsIgnoreCase(args[0]))
                        if (!buffer.contains(HelpCommand.commandList.indexOf(cmd)) && !cmd.getCommandName().equalsIgnoreCase("enable"))
                            buffer.add(HelpCommand.commandList.indexOf(cmd));
                }
                if (buffer.size() == disabledGuildCommands.getOrDefault(guild.getIdLong(), new ArrayList<>()).size()) {
                    event.reply("The given command or category was unknown");
                } else {
                    event.reply("Successfully disabled **" + args[0] + "**");
                    Melijn.MAIN_THREAD.submit(() -> {
                        Melijn.mySQL.addDisabledCommands(guild.getIdLong(), buffer);
                        if (disabledGuildCommands.replace(guild.getIdLong(), buffer) == null)
                            disabledGuildCommands.put(guild.getIdLong(), buffer);
                    });
                }
            } else if (event.getExecutor().equalsIgnoreCase("disabled")) {
                StringBuilder sb = new StringBuilder();
                int part = 1;
                sb.append("Part **#").append(part++).append("** of disabled commands```INI\n");
                for (int i : disabledGuildCommands.getOrDefault(event.getGuild().getIdLong(), new ArrayList<>())) {
                    if (sb.toString().length() + HelpCommand.commandList.get(i).getCommandName().length() < 1950) {
                        sb.append(i).append(" - [").append(HelpCommand.commandList.get(i).getCommandName()).append("]\n");
                    } else {
                        sb.append("```");
                        event.reply(sb.toString());
                        sb = new StringBuilder();
                        sb.append("Part **#").append(part++).append("** of disabled commands```INI");
                    }
                }
                sb.append("```");
                event.reply(sb.toString());
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }
}
