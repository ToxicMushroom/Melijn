package me.melijn.jda.commands.management;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;

import java.util.List;
import java.util.stream.Collectors;

import static me.melijn.jda.Melijn.PREFIX;

public class DisableCommand extends Command {

    public static TLongObjectMap<TIntList> disabledGuildCommands = Melijn.mySQL.getDisabledCommandsMap();

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
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            long guildId = event.getGuild().getIdLong();

            if (event.getExecutor().equalsIgnoreCase("disable") && args.length > 0 && !args[0].isEmpty()) {
                TLongObjectMap<TIntList> map = DisableCommand.disabledGuildCommands;
                TIntList buffer = map.containsKey(guildId) ? map.get(guildId) : new TIntArrayList();

                int sizeBefore = buffer.size();
                for (Command cmd : event.getClient().getCommands()) {

                    if (cmd.getCommandName().equalsIgnoreCase(args[0])) {
                        if (!buffer.contains(cmd.getId()) && !cmd.getCommandName().equalsIgnoreCase("enable")) {
                            buffer.add(cmd.getId());
                        } else {
                            event.reply("**" + cmd.getCommandName() + "** was already disabled");
                        }
                        return;
                    }

                    if (cmd.getCategory().toString().equalsIgnoreCase(args[0]) &&
                            !buffer.contains(cmd.getId()) &&
                            !cmd.getCommandName().equalsIgnoreCase("enable")) {
                        buffer.add(cmd.getId());
                    }

                }
                if (buffer.size() == sizeBefore) {
                    event.reply("The given command or category was unknown");
                } else {
                    event.reply("Successfully disabled **" + args[0] + "**");
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.addDisabledCommands(guildId, buffer);
                        disabledGuildCommands.put(guildId, buffer);
                    });
                }
            } else if (event.getExecutor().equalsIgnoreCase("disabled")) {
                StringBuilder sb = new StringBuilder();
                int part = 1;
                sb.append("Part **#").append(part++).append("** of disabled commands```INI\n");
                for (int i : (disabledGuildCommands.containsKey(event.getGuild().getIdLong()) ? disabledGuildCommands.get(guildId) : new TIntArrayList()).toArray()) {
                    List<Command> match = event.getClient().getCommands().stream().filter(cmd -> cmd.getId() == i).collect(Collectors.toList());
                    if (match.size() == 0) {
                        Melijn.mySQL.removeDisabledCommands(guildId, new TIntArrayList(new int[]{i}));
                        TIntList newList = disabledGuildCommands.get(guildId);
                        newList.remove(i);
                        disabledGuildCommands.put(guildId, newList);
                        continue;
                    }
                    if (sb.length() + match.get(0).getCommandName().length() < 1950) {
                        sb.append(i).append(" - [").append(event.getClient().getCommands().get(i).getCommandName()).append("]\n");
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
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
