package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.MessageType;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class SetLeaveMessageCommand extends Command {

    public SetLeaveMessageCommand() {
        this.commandName = "setLeaveMessage";
        this.description = "Setup a message that a user get's when he/she/it leaves";
        this.usage = Melijn.PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders: `%USER%` = user mention // `%USERNAME%` = user name // `%GUILDNAME%` = your discord server's name // `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"slm"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, String> leaveMessages = Melijn.mySQL.getMessageMap(MessageType.LEAVE);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String oldMessage = leaveMessages.getOrDefault(guild.getIdLong(), "");
                String newMessage = event.getArgs();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    if (args.length == 1 && args[0].equalsIgnoreCase("null")) {
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.removeMessage(guild.getIdLong(), MessageType.JOIN));
                        leaveMessages.remove(guild.getIdLong());
                        event.reply("LeaveMessage has been set to nothing by **" + event.getFullAuthorName() + "**");
                    } else {
                        Melijn.MAIN_THREAD.submit(() -> Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE));
                        if (leaveMessages.replace(guild.getIdLong(), newMessage) == null)
                            leaveMessages.put(guild.getIdLong(), newMessage);
                        event.reply("LeaveMessage has been changed from '" + oldMessage + "' to '" + newMessage + "' by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    event.reply(oldMessage);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
