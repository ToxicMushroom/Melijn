package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.MessageType;
import net.dv8tion.jda.core.entities.Guild;

public class SetLeaveMessageCommand extends Command {

    public SetLeaveMessageCommand() {
        this.commandName = "setLeaveMessage";
        this.description = "Setup a message that a user get's when he/she/it leaves";
        this.usage = Melijn.PREFIX + commandName + " [message | null]";
        this.extra = "Placeholders: `%USER%` = user mention // `%USERNAME%` = user name // `%GUILDNAME%` = your discord server's name // `%JOINPOSITION%` = member position";
        this.aliases = new String[]{"slm"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String newMessage = event.getArgs();
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    if (args.length == 1 && args[0].equalsIgnoreCase("null")) {
                        Melijn.MAIN_THREAD.submit(() -> {
                            String message = Melijn.mySQL.getMessage(guild.getIdLong(), MessageType.LEAVE);
                            Melijn.mySQL.removeMessage(guild.getIdLong(), MessageType.JOIN);
                            event.reply("LeaveMessage has been changed from " + (message == null ? "nothing" : "'" + message + "'") + " to nothing by **" + event.getFullAuthorName() + "**");
                        });
                    } else {
                        Melijn.MAIN_THREAD.submit(() -> {
                            String message = Melijn.mySQL.getMessage(guild.getIdLong(), MessageType.LEAVE);
                            Melijn.mySQL.setMessage(guild.getIdLong(), newMessage, MessageType.LEAVE);
                            event.reply("LeaveMessage has been changed from " + (message == null ? "nothing" : "'" + message + "'") + " to '" + newMessage + "' by **" + event.getFullAuthorName() + "**");
                        });
                    }
                } else {
                    event.reply(Melijn.mySQL.getMessage(guild.getIdLong(), MessageType.LEAVE));
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
