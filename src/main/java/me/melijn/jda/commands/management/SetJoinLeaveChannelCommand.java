package me.melijn.jda.commands.management;

import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class SetJoinLeaveChannelCommand extends Command {


    public SetJoinLeaveChannelCommand() {
        this.commandName = "setJoinLeaveChannel";
        this.description = "Sets a TextChannel where users will be welcomed or bid farewell";
        this.usage = PREFIX + commandName + " <TextChannel | null>";
        this.aliases = new String[]{"sjlc", "SetWelcomeChannel", "swc"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 35;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            long welcomeChannelId = event.getVariables().welcomeChannelCache.getUnchecked(guild.getIdLong());
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                long id = event.getHelpers().getTextChannelByArgsN(event, args[0]);
                if (id == -1L) {
                    event.reply("Unknown TextChannel");
                } else if (id == 0L) {
                    event.async(() -> {
                        event.getMySQL().removeChannel(guild.getIdLong(), ChannelType.WELCOME);
                        event.getVariables().welcomeChannelCache.invalidate(guild.getIdLong());
                    });
                    long oldChannel = event.getVariables().welcomeChannelCache.getUnchecked(guild.getIdLong());
                    event.reply("WelcomeChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                } else {
                    event.async(() -> {
                        event.getMySQL().setChannel(guild.getIdLong(), id, ChannelType.WELCOME);
                        event.getVariables().welcomeChannelCache.put(guild.getIdLong(), id);
                        if (event.getVariables().joinMessages.getUnchecked(guild.getIdLong()).isEmpty()) {
                            event.getMySQL().setMessage(guild.getIdLong(), "Welcome **%USER%** to our awesome discord server :D", MessageType.JOIN);
                            event.getVariables().joinMessages.put(guild.getIdLong(), "Welcome %USER% to the %GUILDNAME% discord server");
                        }
                        if (event.getVariables().leaveMessages.getUnchecked(guild.getIdLong()).isEmpty()) {
                            event.getMySQL().setMessage(guild.getIdLong(), "**%USERNAME%** left us :C", MessageType.LEAVE);
                            event.getVariables().leaveMessages.put(guild.getIdLong(), "**%USERNAME%** left us :C");
                        }
                        event.reply("I've set the default join and leave message :beginner:");

                        String oldChannel = welcomeChannelId == -1 ? "nothing" : "<#" + welcomeChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
                    });
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
    }
}
