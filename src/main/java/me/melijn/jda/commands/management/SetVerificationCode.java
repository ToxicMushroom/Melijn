package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class SetVerificationCode extends Command {

    public SetVerificationCode() {
        this.commandName = "setVerificationCode";
        this.usage = Melijn.PREFIX + commandName + " [code | null]";
        this.description = "set's a verificationCode that users will have to send in the verificationChannel";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    public static HashMap<Long, String> guildCodes = Melijn.mySQL.getVerificationCodeMap();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            if (guild.getTextChannelById(SetVerificationChannel.verificationChannelsCache.getUnchecked(guild.getIdLong())) != null) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    if (args[0].equalsIgnoreCase("null")) {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.removeVerificationCode(guild.getIdLong());
                            guildCodes.remove(guild.getIdLong());
                        });
                        event.reply("The VerificationCode has been set to nothing by **" + event.getFullAuthorName() + "**");
                    } else {
                        TaskScheduler.async(() -> {
                            Melijn.mySQL.setVerificationCode(guild.getIdLong(), args[0]);
                            if (guildCodes.replace(guild.getIdLong(), args[0]) == null)
                                guildCodes.put(guild.getIdLong(), args[0]);
                        });
                        event.reply("The VerificationCode has been set to " + args[0] + " by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    event.reply("The VerificationCode is " + guildCodes.getOrDefault(guild.getIdLong(), "unset"));
                }
            } else {
                event.reply("You first have to setup a Verification TextChannel\nYou'll probably want to follow this guide: https://melijn.com/guides/guide-7");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
