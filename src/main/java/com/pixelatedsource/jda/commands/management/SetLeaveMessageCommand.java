package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.MessageType;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetLeaveMessageCommand extends Command {

    public SetLeaveMessageCommand() {
        this.commandName = "setleavemessage";
        this.description = "Setup a message that a user get's when he/she/it leaves\nPlaceholders: `%USER%` = user mention // `%USERNAME%` = user name";
        this.usage = PREFIX + commandName + " <message>";
        this.aliases = new String[]{"slm"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, String> leaveMessages = PixelSniper.mySQL.getMessageMap(MessageType.LEAVE);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String oldMessage = leaveMessages.getOrDefault(guild.getIdLong(), "");
                String newMessage = event.getArgs();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    new Thread(() -> PixelSniper.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE)).start();
                    if (leaveMessages.replace(guild.getIdLong(), newMessage) == null)
                        leaveMessages.put(guild.getIdLong(), newMessage);
                    event.reply("LeaveMessage has been changed from '" + oldMessage + "' to '" + newMessage + "'");
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
