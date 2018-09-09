package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import static me.melijn.jda.Melijn.PREFIX;

public class MetricsCommand extends Command {

    public MetricsCommand() {
        this.commandName = "metrics";
        this.description = "Shows information about command usage";
        this.usage = PREFIX + commandName + " <limit|command|category|all> [limit] <timespan>";
        this.category = Category.UTILS;
        this.extra = "Timespan: thisHour, hour, day, today, week, month, all or dd/mm/yyyy - dd/mm/yyyy or hh:mm - hh:mm or hh:mm-dd/mm/yyyy - hh:mm-dd/mm/yyyy";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length >= 2) {
                StringBuilder sb = new StringBuilder("```INI\n");
                int defaultInt = 10;
                long[] period;
                if (args[1].matches("[0-9]{1,2}")) {
                    defaultInt = Integer.parseInt(args[1]);
                    period = parseTimes(event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "(\\s+)?", ""));
                } else {
                    period = parseTimes(event.getArgs().replaceFirst(args[0] + "(\\s+)?", ""));
                }
                if (period != null) {
                    switch (args[0]) {
                        case "top":
                        case "limit":
                            HashMap<Integer, Long> topCommandUsage = Melijn.mySQL.getTopUsage(period, defaultInt);
                            for (int id : topCommandUsage.keySet()) {
                                sb.append(topCommandUsage.get(id)).append(" - [").append(event.getClient().getCommands().get(id).getCommandName()).append("]\n");
                            }
                            break;
                        case "all":
                            HashMap<Integer, Long> allCommandUsage = Melijn.mySQL.getTopUsage(period, event.getClient().getCommands().size());
                            for (int id : allCommandUsage.keySet()) {
                                sb.append(allCommandUsage.get(id)).append(" - [").append(event.getClient().getCommands().get(id).getCommandName()).append("]\n");
                            }
                            break;
                        default:
                            ArrayList<Integer> commandIds = new ArrayList<>();
                            for (Command command : event.getClient().getCommands()) {
                                if (command.getCommandName().equalsIgnoreCase(args[0])) {
                                    sb.append(Melijn.mySQL.getUsage(period, event.getClient().getCommands().indexOf(command))).append(" - [").append(command.getCommandName()).append("]\n");
                                } else if (command.getCategory().toString().equalsIgnoreCase(args[0])) {
                                    commandIds.add(event.getClient().getCommands().indexOf(command));
                                }
                            }
                            if (commandIds.size() > 0) {
                                HashMap<Integer, Long> commandUsages = Melijn.mySQL.getUsages(period, commandIds);
                                for (int index : commandUsages.keySet()) {
                                    sb.append(commandUsages.get(index)).append(" - [").append(event.getClient().getCommands().get(index).getCommandName()).append("]\n");
                                }
                            }
                            break;
                    }
                    sb.append("```");
                    event.reply(sb.toString());
                } else {
                    event.reply("Invalid timespan");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }

    private long[] parseTimes(String text) {
        if (text.split("\\s+").length > 1) {
            String[] times = text.split("(\\s+)?--(\\s+)?");
            if (times.length == 2) {
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
                return null;
            }
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
}
