package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class AntiRaidCommand extends Command {

    public AntiRaidCommand() {
        this.commandName = "antiRaid";
        this.description = "Management command for AntiRaid";
        this.usage = PREFIX + commandName + " threshold <users per hour | -1>";
        this.aliases = new String[]{"ar"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 116;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getHelpers().hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length < 1) {
                event.getMessageHelper().sendUsage(this, event);
                return;
            }

            if (args[0].equalsIgnoreCase("threshold")) {
                if (args.length > 1 && args[1].length() < 6 && args[1].matches("\\d+")) {
                    long threshold = Long.parseLong(args[1]);
                    event.getMySQL().setAntiRaidThreshold(event.getGuildId(), threshold);
                    event.getVariables().antiRaidThresholdChache.put(event.getGuildId(), threshold);
                    event.reply("The raid detection threshold has been set to **" + threshold + "**");
                } else {
                    long threshold = event.getVariables().antiRaidThresholdChache.getUnchecked(event.getGuildId());
                    event.reply("Current max users per hour: **" + (threshold == -1 ? "infinite" : threshold) + "**");
                }
            } else {
                event.getMessageHelper().sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
