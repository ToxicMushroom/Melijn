package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationCodeCommand extends Command {


    public SetVerificationCodeCommand() {
        this.commandName = "setVerificationCode";
        this.usage = PREFIX + commandName + " [code | null]";
        this.description = "Sets the VerificationCode that members will have to send in the VerificationChannel in order to get verified";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 7;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            if (guild.getTextChannelById(event.getVariables().verificationChannelsCache.getUnchecked(guild.getIdLong())) == null) {
                event.reply("" +
                        "You first have to setup a Verification TextChannel\n" +
                        "You'll probably want to follow this guide: https://melijn.com/guides/guide-7"
                );
                return;
            }
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                if (args[0].equalsIgnoreCase("null")) {
                    event.async(() -> {
                        event.getMySQL().removeVerificationCode(guild.getIdLong());
                        event.getVariables().verificationCodeCache.invalidate(guild.getIdLong());
                    });
                    event.reply("The VerificationCode has been set to nothing by **" + event.getFullAuthorName() + "**");
                } else {
                    event.async(() -> {
                        event.getMySQL().setVerificationCode(guild.getIdLong(), args[0]);
                        event.getVariables().verificationCodeCache.put(guild.getIdLong(), args[0]);
                    });
                    event.reply("The VerificationCode has been set to " + args[0] + " by **" + event.getFullAuthorName() + "**");
                }
            } else {
                String value = (event.getVariables().verificationCodeCache.getUnchecked(guild.getIdLong()) == null ?
                        "unset" :
                        event.getVariables().verificationCodeCache.getUnchecked(guild.getIdLong()));
                event.reply("The VerificationCode is **" + value + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
