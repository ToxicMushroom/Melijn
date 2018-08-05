package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class SetVerificationChannel extends Command {

    public SetVerificationChannel() {
        this.commandName = "setVerificationChannel";
        this.description = "set a channel in which the user will have to prove that he or she is not a bot by answering a captcha or question";
        this.aliases = new String[]{"svc"};
        this.extra = "You can manually approve users by using the verify command";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    public static HashMap<Long, Long> verificationChannels = PixelSniper.mySQL.getChannelMap(ChannelType.VERIFICATION);

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            long verificationChannelId = verificationChannels.getOrDefault(guild.getIdLong(), -1L);
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                long id = Helpers.getTextChannelByArgsN(event, args[0]);
                if (id == -1L) {
                    event.reply("Unknown TextChannel");
                } else if (id == 0L) {
                    verificationChannels.remove(guild.getIdLong());
                    new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION)).start();
                    long oldChannel = verificationChannels.getOrDefault(guild.getIdLong(), -1L);
                    event.reply("VerificationChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                } else  {
                    if (event.getGuild().getSelfMember().hasPermission(guild.getTextChannelById(id), Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE)) {
                        new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.VERIFICATION)).start();
                        if (verificationChannels.replace(guild.getIdLong(), id) == null)
                            verificationChannels.put(guild.getIdLong(), id);

                        String oldChannel = verificationChannelId == -1 ? "nothing" : "<#" + verificationChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("VerificationChannel has been changed from " + oldChannel + " to " + newChannel);

                        if (!SetJoinRoleCommand.joinRoles.containsKey(event.getGuild().getIdLong())) {
                            event.reply("Now it's time to set the role that people will receive after verification to get access to other channels\nUse >setJoinRole <role>");
                        }
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
