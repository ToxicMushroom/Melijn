package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;
import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetVerificationCode extends Command {

    public SetVerificationCode() {
        this.commandName = "setVerificationCode";
        this.usage = PREFIX + commandName + " [code | null]";
        this.description = "set's a verificationCode that users will have to send in the verificationChannel";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    public static HashMap<Long, String> guildCodes = PixelSniper.mySQL.getVerificationCodeMap();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            if (guild.getTextChannelById(SetVerificationChannel.verificationChannels.getOrDefault(guild.getIdLong(), -1L)) != null) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    if (args[0].equalsIgnoreCase("null")) {
                        new Thread(() -> {
                            PixelSniper.mySQL.removeVerificationCode(guild.getIdLong());
                            guildCodes.remove(guild.getIdLong());
                        }).start();
                        event.reply("The VerificationCode has been set to nothing by **" + event.getFullAuthorName() + "**");
                    } else {
                        new Thread(() -> {
                            PixelSniper.mySQL.setVerificationCode(guild.getIdLong(), args[0]);
                            if (guildCodes.replace(guild.getIdLong(), args[0]) == null)
                                guildCodes.put(guild.getIdLong(), args[0]);
                        }).start();
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
