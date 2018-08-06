package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

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

    public static HashMap<Long, Integer> guildVerificationThresholds = PixelSniper.mySQL.getGuildVerificationThresholdMap();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && args[0].matches("\\d+") && args[0].length() < 4) {
                int i = Integer.parseInt(args[0]);
                if (i <= 20 && i >= 0) {
                    Guild guild = event.getGuild();
                    if (i == 0) {
                        new Thread(() -> {
                            PixelSniper.mySQL.removeVerificationThreshold(guild.getIdLong());
                            guildVerificationThresholds.remove(guild.getIdLong());
                        }).start();
                        event.reply("The VerificationThreshold has been disabled by **" + event.getFullAuthorName() + "**");
                    } else {
                        new Thread(() -> {
                            PixelSniper.mySQL.setVerificationThreshold(guild.getIdLong(), i);
                            if (guildVerificationThresholds.replace(guild.getIdLong(), i) == null)
                                guildVerificationThresholds.put(guild.getIdLong(), i);
                        }).start();
                        event.reply("The VerificationThreshold has been set to **" + i + "** by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
