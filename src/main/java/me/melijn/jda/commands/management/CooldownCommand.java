package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gnu.trove.map.TIntIntMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static me.melijn.jda.Melijn.PREFIX;

public class CooldownCommand extends Command {

    public static final Cooldown activeCooldowns = new Cooldown();// Guild -> User -> command -> time used
    public static final LoadingCache<Long, TIntIntMap> cooldowns = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public TIntIntMap load(@NotNull Long key) {
                    return Melijn.mySQL.getCooldowns(key);
                }
            });

    public CooldownCommand() {
        this.commandName = "cooldown";
        this.usage = PREFIX + commandName + " <set | remove | list>";
        this.description = "Main command to configure cooldowns for each command";
        this.extra = "Users with the bypass.cooldown permission will not be affected by cooldowns";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 108;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0) {
                MessageHelper.sendUsage(this, event);
                return;
            }
            switch (args[0].toLowerCase()) {
                case "set":
                    if (args.length != 3) {
                        event.reply(SetPrefixCommand.prefixes.getUnchecked(event.getGuildId()) + commandName + " set <command | category | all> <1-30000 (milliseconds)>");
                        return;
                    }
                    List<Command> matches = event.getClient().getCommands().stream()
                            .filter(cmd ->
                                    cmd.getCommandName().equalsIgnoreCase(args[1]) ||
                                    Arrays.asList(cmd.getAliases()).contains(args[1]) ||
                                    cmd.getCategory().toString().equalsIgnoreCase(args[1]) ||
                                    args[1].equalsIgnoreCase("all"))
                            .collect(Collectors.toList());
                    if (matches.size() == 0) {
                        event.reply("Unknown commandName or alias\nTip: check https://melijn.com/commands for commandNames");
                        return;
                    }
                    if (!args[2].matches("\\d{1,5}") || Integer.parseInt(args[2]) > 30_000 || Integer.parseInt(args[2]) < 1) {
                        event.reply("Your input was not a number or was not in range of **1-30000** (milliseconds)");
                        return;
                    }
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setCooldown(event.getGuildId(), matches, Integer.parseInt(args[2]));
                        TIntIntMap map = cooldowns.getUnchecked(event.getGuildId());
                        matches.forEach(command -> map.put(command.getId(), Integer.parseInt(args[2])));
                        cooldowns.put(event.getGuildId(), map);
                    });
                    event.reply("The cooldown of **" + args[1] + "** has been set to **" + Integer.parseInt(args[2]) + "ms**");
                    break;
                case "remove":
                    if (args.length != 2) {
                        event.reply(SetPrefixCommand.prefixes.getUnchecked(event.getGuildId()) + commandName + " remove <command | category | all>");
                        return;
                    }
                    matches = event.getClient().getCommands().stream()
                            .filter(cmd ->
                                    cmd.getCommandName().equalsIgnoreCase(args[1]) ||
                                    Arrays.asList(cmd.getAliases()).contains(args[1]) ||
                                    cmd.getCategory().toString().equalsIgnoreCase(args[1]) ||
                                    args[1].equalsIgnoreCase("all"))
                            .collect(Collectors.toList());
                    if (matches.size() == 0) {
                        event.reply("Unknown commandName or alias\nTip: check https://melijn.com/commands for commandNames");
                        return;
                    }
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeCooldown(event.getGuildId(), matches);
                        TIntIntMap map = cooldowns.getUnchecked(event.getGuildId());
                        matches.forEach(cmd -> map.remove(cmd.getId()));
                        cooldowns.put(event.getGuildId(), map);
                    });
                    event.reply("The cooldown of **" + args[1] + "** has been removed");
                    break;
                case "list":
                    TIntIntMap map = cooldowns.getUnchecked(event.getGuildId());
                    StringBuilder sb = new StringBuilder();
                    AtomicInteger i = new AtomicInteger(1);
                    map.forEachEntry((a, b) -> {
                        List<Command> match = event.getClient().getCommands().stream().filter(cmd -> cmd.getId() == a).collect(Collectors.toList());
                        if (match.size() > 0)
                            sb.append(i.getAndIncrement()).append(". ").append(match.get(0).getCommandName()).append(" ").append(b).append("ms\n");
                        return true; //can proceed
                    });
                    if (sb.toString().length() == 0) event.reply("Looks empty to me");
                    else MessageHelper.sendSplitCodeBlock(event.getTextChannel(), sb.toString(), "Markdown");
                    break;
                default:
                    MessageHelper.sendUsage(this, event);
                    break;
            }
        }
    }
}
