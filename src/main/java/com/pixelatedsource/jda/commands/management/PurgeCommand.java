package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PurgeCommand extends Command {

    public PurgeCommand() {
        this.commandName = "purge";
        this.description = "Deletes messages messages";
        this.usage = PREFIX + commandName + " [1-100]";
        this.category = Category.MANAGEMENT;
    }
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length == 1 && !args[0].equalsIgnoreCase("") && args[0].matches("\\d+")) {
                        try {
                            Runnable runnable = () -> {
                                Long start = System.currentTimeMillis();
                                int size = event.getMessage().getTextChannel().getHistory().retrievePast(Integer.parseInt(args[0])).complete().size();
                                int progress = 0;
                                List<Message> list = event.getMessage().getTextChannel().getHistory().retrievePast(Integer.parseInt(args[0])).complete();
                                Message purgingMessage = event.getTextChannel().sendMessage("Purging... 0% 0s").complete();
                                for (Message message : list) {
                                    progress++;
                                    message.delete().complete();
                                    purgingMessage.editMessage("Purging... " + Math.round((progress / size * 100)) + "% - " + Math.round((System.currentTimeMillis() - start) / 1000) + "s").complete();
                                }
                                purgingMessage.editMessage("Purged in " + Math.round((System.currentTimeMillis() - start) / 1000) + "s").complete();
                            };
                            executorService.execute(runnable);
                        } catch (NumberFormatException e) {
                            event.reply("Error: NumberFormatException");
                        }
                    } else {
                        if (event.getGuild() != null) {
                            event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                        } else {
                            event.reply(usage);
                        }
                    }
                } else {
                    event.reply("I have no permission to manage messages.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
