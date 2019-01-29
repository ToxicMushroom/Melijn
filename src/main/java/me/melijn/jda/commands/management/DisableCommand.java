package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.melijn.jda.Melijn.PREFIX;

public class DisableCommand extends Command {

    public DisableCommand() {
        this.commandName = "disable";
        this.description = "Fully disables commands from being used";
        this.usage = PREFIX + commandName + " <commandName | category>";
        this.needs = new Need[]{Need.GUILD};
        this.aliases = new String[]{"disabled"};
        this.extra = "You can use >disabled to get a list";
        this.category = Category.MANAGEMENT;
        this.id = 87;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            long guildId = event.getGuild().getIdLong();

            if (event.getExecutor().equalsIgnoreCase("disable") && args.length > 0 && !args[0].isEmpty()) {
                Map<Long, List<Integer>> map = new HashMap<>(event.getVariables().disabledGuildCommands);
                List<Integer> buffer = new ArrayList<>(map.containsKey(guildId) ? map.get(guildId) : new ArrayList<>());

                int sizeBefore = buffer.size();
                for (Command cmd : event.getClient().getCommands()) {
                    if (cmd.isCommandFor("enable")) return;
                    if (cmd.isCommandFor(args[0])) {
                        if (buffer.contains(cmd.getId())) {
                            event.reply("**" + cmd.getCommandName() + "** was already disabled");
                        } else {
                            buffer.add(cmd.getId());
                        }
                        break;
                    }

                    if (cmd.getCategory().toString().equalsIgnoreCase(args[0]) && !buffer.contains(cmd.getId())) {
                        buffer.add(cmd.getId());
                    }

                }
                if (buffer.size() == sizeBefore) {
                    event.reply("The given command or category was unknown");
                } else {
                    event.reply("Successfully disabled **" + args[0] + "**");
                    event.async(() -> {
                        event.getMySQL().addDisabledCommands(guildId, buffer);
                        event.getVariables().disabledGuildCommands.put(guildId, buffer);
                    });
                }
            } else if (event.getExecutor().equalsIgnoreCase("disabled")) {
                StringBuilder sb = new StringBuilder();
                int part = 1;
                sb.append("Part **#").append(part++).append("** of disabled commands```INI\n");
                for (int i : (event.getVariables().disabledGuildCommands.containsKey(event.getGuild().getIdLong()) ? event.getVariables().disabledGuildCommands.get(guildId) : new ArrayList<Integer>())) {
                    List<Command> match = event.getClient().getCommands().stream().filter(cmd -> cmd.getId() == i).collect(Collectors.toList());
                    if (match.size() == 0) {
                        event.getMySQL().removeDisabledCommands(guildId, new ArrayList<>(i));
                        List<Integer> newList = event.getVariables().disabledGuildCommands.get(guildId);
                        newList.remove(i);
                        event.getVariables().disabledGuildCommands.put(guildId, newList);
                        continue;
                    }
                    if (sb.length() + match.get(0).getCommandName().length() < 1950) {
                        sb.append(i).append(" - [").append(match.get(0).getCommandName()).append("]\n");
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
                event.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
