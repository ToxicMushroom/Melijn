package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import static me.melijn.jda.Melijn.PREFIX;

public class MetricsCommand extends Command {

    public MetricsCommand() {
        this.commandName = "metrics";
        this.description = "Shows information about command usage";
        this.usage = PREFIX + commandName + " <limit|command|category|all> [limit] <timespan>";
        this.category = Category.UTILS;
        this.extra = "Timespan: thisHour, hour, day, today, week, month, all or dd/mm/yyyy - dd/mm/yyyy or hh:mm - hh:mm or hh:mm-dd/mm/yyyy - hh:mm-dd/mm/yyyy";
        this.id = 88;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length < 2) {
                event.sendUsage(this, event);
                return;
            }
            StringBuilder sb = new StringBuilder("```INI\n");
            int defaultInt = 10;
            long[] period;
            if (args[1].matches("[0-9]{1,2}")) {
                defaultInt = Integer.parseInt(args[1]);
                period = parseTimes(event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "(\\s+)?", ""));
            } else {
                period = parseTimes(event.getArgs().replaceFirst(args[0] + "(\\s+)?", ""));
            }
            if (period == null) {
                event.reply("Invalid timespan");
                return;
            }
            switch (args[0]) {
                case "top":
                case "limit":
                    LinkedHashMap<Integer, Long> topCommandUsage = event.getMySQL().getTopUsage(period, defaultInt);
                    for (int id : topCommandUsage.keySet()) {
                        sb.append(topCommandUsage.get(id)).append(" - [").append(getCommandById(event, id).getCommandName()).append("]\n");
                    }
                    break;
                case "all":
                    LinkedHashMap<Integer, Long> allCommandUsage = event.getMySQL().getTopUsage(period, event.getClient().getCommands().size());
                    for (int id : allCommandUsage.keySet()) {
                        sb.append(allCommandUsage.get(id)).append(" - [").append(getCommandById(event, id).getCommandName()).append("]\n");
                    }
                    break;
                default:
                    List<Integer> commandIds = new ArrayList<>();
                    for (Command command : event.getClient().getCommands()) {
                        if (command.getCommandName().equalsIgnoreCase(args[0])) {
                            sb.append(event.getMySQL().getUsage(period, command.getId())).append(" - [").append(command.getCommandName()).append("]\n");
                        } else if (command.getCategory().toString().equalsIgnoreCase(args[0])) {
                            commandIds.add(command.getId());
                        }
                    }
                    if (commandIds.size() > 0) {
                        Map<Integer, Long> commandUsages = event.getMySQL().getUsages(period, commandIds);
                        for (int id : commandUsages.keySet()) {
                            sb.append(commandUsages.get(id)).append(" - [").append(getCommandById(event, id).getCommandName()).append("]\n");
                        }
                    }
                    break;
            }
            sb.append("```");
            event.reply(sb.toString());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private long[] parseTimes(String text) {
        if (text.split("\\s+").length > 1) {
            String[] times = text.split("(\\s+)?--(\\s+)?");
            if (times.length != 2) return null;
            long first = 0;
            long second = 0;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm-dd/MM/yyyy");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Brussels")));
            if (times[0].equalsIgnoreCase("now")) first = System.currentTimeMillis();
            else if (times[0].matches("((([0-2])?([0-9]))|([0-3][0-1]))/(((0)?[0-9])|([0-1][0-2]))/(([0-1]?[0-9]{1,3})|(20[0-2][0-9]))")) {
                try {
                    first = simpleDateFormat.parse("00:00-" + times[0]).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (times[0].matches("((([0-1]?[0-9])|(2[0-3])):([0-5]?[0-9]))-((([0-2])?([0-9]))|([0-3][0-1]))/(((0)?[0-9])|([0-1][0-2]))/(([0-1]?[0-9]{1,3})|(20[0-2][0-9]))")) {
                try {
                    first = simpleDateFormat.parse(times[0]).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (times[1].equalsIgnoreCase("now")) second = System.currentTimeMillis();
            else if (times[1].matches("((([0-2])?([0-9]))|([0-3][0-1]))/(((0)?[0-9])|([0-1][0-2]))/(([0-1]?[0-9]{1,3})|(20[0-2][0-9]))")) {
                try {
                    second = simpleDateFormat.parse("00:00-" + times[1]).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (times[1].matches("((([0-1]?[0-9])|(2[0-3])):([0-5]?[0-9]))-((([0-2])?([0-9]))|([0-3][0-1]))/(((0)?[0-9])|([0-1][0-2]))/(([0-1]?[0-9]{1,3})|(20[0-2][0-9]))")) {
                try {
                    second = simpleDateFormat.parse(times[1]).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return new long[]{first, second};
        } else {
            if (text.equalsIgnoreCase("today")) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                return new long[]{System.currentTimeMillis(), c.getTimeInMillis()};
            } else if (text.equalsIgnoreCase("day")) {
                return new long[]{System.currentTimeMillis(), System.currentTimeMillis() - 86_400_000};
            } else if (text.equalsIgnoreCase("thisHour")) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                return new long[]{System.currentTimeMillis(), c.getTimeInMillis()};
            } else if (text.equalsIgnoreCase("hour")) {
                return new long[]{System.currentTimeMillis(), System.currentTimeMillis() - 3_600_000};
            } else if (text.equalsIgnoreCase("week")) {
                return new long[]{System.currentTimeMillis(), System.currentTimeMillis() - 604_800_000};
            } else if (text.equalsIgnoreCase("month")) {
                return new long[]{System.currentTimeMillis(), System.currentTimeMillis() - 2_629_746_000L};
            } else if (text.equalsIgnoreCase("all")) {
                return new long[]{System.currentTimeMillis(), 0};
            } else return null;
        }
    }

    public Command getCommandById(CommandEvent event, long id) {
        Optional<Command> command = event.getClient().getCommands().stream().filter(cmd -> cmd.getId() == id).findAny();
        return command.orElse(null);
    }
}
