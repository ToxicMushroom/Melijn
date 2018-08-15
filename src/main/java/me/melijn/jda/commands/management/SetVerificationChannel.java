package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationChannel extends Command {

    public SetVerificationChannel() {
        this.commandName = "setVerificationChannel";
        this.usage = PREFIX + commandName + " [TextChannel | null]";
        this.description = "set a channel in which the user will have to prove that he or she is not a bot by answering a captcha or question";
        this.aliases = new String[]{"svc"};
        this.extra = "You can manually approve users by using the verify command";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    public static HashMap<Long, Long> verificationChannels = Melijn.mySQL.getChannelMap(ChannelType.VERIFICATION);

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
                    long oldChannel = verificationChannels.getOrDefault(guild.getIdLong(), -1L);
                    event.reply("VerificationChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    verificationChannels.remove(guild.getIdLong());
                    Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.VERIFICATION));
                } else  {
                    if (event.getGuild().getSelfMember().hasPermission(guild.getTextChannelById(id), Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE)) {
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.VERIFICATION));
                        if (verificationChannels.replace(guild.getIdLong(), id) == null)
                            verificationChannels.put(guild.getIdLong(), id);

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
