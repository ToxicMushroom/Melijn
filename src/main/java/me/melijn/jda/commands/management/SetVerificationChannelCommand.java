package me.melijn.jda.commands.management;

import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationChannelCommand extends Command {



    public SetVerificationChannelCommand() {
        this.commandName = "setVerificationChannel";
        this.usage = PREFIX + commandName + " [TextChannel | null]";
        this.description = "Sets the channel in which the members will have to prove that they are not a bot by entering the VerificationCode";
        this.aliases = new String[]{"svc"};
        this.extra = "You can manually approve users by using the verify command";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 8;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            long verificationChannelId = event.getVariables().verificationChannelsCache.get(guild.getIdLong());
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                long id = event.getHelpers().getTextChannelByArgsN(event, args[0]);
                if (id == -1L) {
                    event.reply("Unknown TextChannel");
                } else if (id == 0L) {
                    long oldChannel = event.getVariables().verificationChannelsCache.get(guild.getIdLong());
                    event.reply("VerificationChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    event.async(() -> {
                        event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.VERIFICATION);
                        event.getVariables().verificationChannelsCache.invalidate(guild.getIdLong());
                    });
                } else {
                    if (event.getGuild().getSelfMember().hasPermission(guild.getTextChannelById(id), Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE)) {
                        event.async(() -> {
                            event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.VERIFICATION);
                            event.getVariables().verificationChannelsCache.put(guild.getIdLong(), id);
                        });
                        String oldChannel = verificationChannelId == -1 ? "nothing" : "<#" + verificationChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("VerificationChannel has been changed from " + oldChannel + " to " + newChannel);
                    } else {
                        event.reply("I need to be able to attach files and manage messages in <#" + verificationChannelId + "> in order to set it as my VerificationChannel\nYou might also want to give me kick permissions so bot's get kicked after 3 tries/incorrect answers");
                    }
                }
            } else {
                if (verificationChannelId != -1)
                    event.reply("Current VerificationChannel: <#" + verificationChannelId + ">");
                else
                    event.reply("Current VerificationChannel is unset");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
