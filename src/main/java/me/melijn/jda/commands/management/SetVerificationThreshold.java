package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationThreshold extends Command {

    public SetVerificationThreshold() {
        this.commandName = "setVerificationThreshold";
        this.usage = PREFIX + commandName + " <0 - 20>";
        this.description = "Set the verification threshold before kicking";
        this.aliases = new String[]{"svt"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.extra = "0 disables the threshold any higher number is the amount of times the user can answer incorrect before being hit with stick";
    }

    public static HashMap<Long, Integer> guildVerificationThresholds = Melijn.mySQL.getGuildVerificationThresholdMap();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length > 0 && args[0].matches("\\d+") && args[0].length() < 4) {
                int i = Integer.parseInt(args[0]);
                if (i <= 20 && i >= 0) {
                    if (i == 0) {
                        Melijn.MAIN_THREAD.submit(() -> {
                            Melijn.mySQL.removeVerificationThreshold(guild.getIdLong());
                            guildVerificationThresholds.remove(guild.getIdLong());
                        });
                        event.reply("The VerificationThreshold has been disabled by **" + event.getFullAuthorName() + "**");
                    } else {
                        Melijn.MAIN_THREAD.submit(() -> {
                            Melijn.mySQL.setVerificationThreshold(guild.getIdLong(), i);
                            if (guildVerificationThresholds.replace(guild.getIdLong(), i) == null)
                                guildVerificationThresholds.put(guild.getIdLong(), i);
                        });
                        event.reply("The VerificationThreshold has been set to **" + i + "** by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("The VerificationThreshold is **" + (guildVerificationThresholds.containsKey(guild.getIdLong()) ? guildVerificationThresholds.get(guild.getIdLong()) : "disabled") + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
