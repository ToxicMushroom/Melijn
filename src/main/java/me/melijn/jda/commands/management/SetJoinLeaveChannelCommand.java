package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class SetJoinLeaveChannelCommand extends Command {

    public SetJoinLeaveChannelCommand() {
        this.commandName = "setJoinLeaveChannel";
        this.description = "Setup a TextChannel where users will be welcomed or leave";
        this.usage = Melijn.PREFIX + commandName + " <TextChannel | null>";
        this.aliases = new String[]{"sjlc", "SetWelcomeChannel", "swc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> welcomeChannels = Melijn.mySQL.getChannelMap(ChannelType.WELCOME);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                long welcomeChannelId = welcomeChannels.getOrDefault(guild.getIdLong(), -1L);
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    long id = Helpers.getTextChannelByArgsN(event, args[0]);
                    if (id == -1L) {
                        event.reply("Unknown TextChannel");
                    } else if (id == 0L) {
                        welcomeChannels.remove(guild.getIdLong());
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.WELCOME));
                        long oldChannel = welcomeChannels.getOrDefault(guild.getIdLong(), -1L);
                        event.reply("WelcomeChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    } else {
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WELCOME));
                        if (!SetJoinMessageCommand.joinMessages.containsKey(guild.getIdLong())) {
                            SetJoinMessageCommand.joinMessages.put(guild.getIdLong(), "Welcome %USER% to the %GUILDNAME% discord server");
                            Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setMessage(guild.getIdLong(), "Welcome %USER% to our awesome discord server :D", MessageType.JOIN));
                            event.reply("I've set the default join message :beginner:");
                        }
                        if (!SetLeaveMessageCommand.leaveMessages.containsKey(guild.getIdLong())) {
                            SetLeaveMessageCommand.leaveMessages.put(guild.getIdLong(), "**%USERNAME%** left us :C");
                            Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setMessage(guild.getIdLong(), "**%USERNAME%** left us :C", MessageType.LEAVE));
                            event.reply("I've set the default leave message :beginner:");
                        }
                        if (welcomeChannels.replace(guild.getIdLong(), id) == null)
                            welcomeChannels.put(guild.getIdLong(), id);

                        String oldChannel = welcomeChannelId == -1  ? "nothing" : "<#" + welcomeChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
                    }
                } else {
                    if (welcomeChannelId != -1)
                        event.reply("Current WelcomeChannel: <#" + welcomeChannelId + ">");
                    else
                        event.reply("Current WelcomeChannel is unset");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }


}
