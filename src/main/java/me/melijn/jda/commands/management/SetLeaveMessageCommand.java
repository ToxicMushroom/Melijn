package me.melijn.jda.commands.management;

import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

public class SetLeaveMessageCommand extends Command {

    public SetLeaveMessageCommand() {
        this.commandName = "setLeaveMessage";
        this.description = "Sets the message that will be sent in the WelcomeChannel when a user leaves";
        this.usage = PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders: `%USER%` = user mention // `%USERNAME%` = user name // `%GUILDNAME%` = your discord server's name // `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"slm"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 38;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String oldMessage = event.getVariables().leaveMessages.get(guild.getIdLong()).isEmpty() ?
                    "nothing" :
                    ("'" + event.getVariables().leaveMessages.get(guild.getIdLong()) + "'");
            String newMessage = event.getArgs();
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                if (args.length == 1 && newMessage.equalsIgnoreCase("null")) {
                    event.async(() -> {
                        event.getMySQL().removeMessage(guild.getIdLong(), MessageType.LEAVE);
                        event.getVariables().leaveMessages.invalidate(guild.getIdLong());
                    });
                    event.getMessageHelper().sendSplitMessage(event.getTextChannel(), "LeaveMessage has been changed from \n" + oldMessage + "\n to nothing by **" + event.getFullAuthorName() + "**");
                } else {
                    event.async(() -> {
                        event.getMySQL().setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE);
                        event.getVariables().leaveMessages.put(guild.getIdLong(), newMessage);
                    });
                    event.getMessageHelper().sendSplitMessage(event.getTextChannel(),
                            "LeaveMessage has been changed from \n" + oldMessage + "\n to \n" + newMessage + "\nby **" + event.getFullAuthorName() + "**");
                }
            } else {
                event.getMessageHelper().sendSplitMessage(event.getTextChannel(), "LeaveMessage is set to:\n" + oldMessage);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
