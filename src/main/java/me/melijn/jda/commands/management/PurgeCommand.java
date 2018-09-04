package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Collector;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class PurgeCommand extends Command {

    public PurgeCommand() {
        this.commandName = "purge";
        this.description = "Deletes messages";
        this.usage = PREFIX + commandName + " [1 - 1000]";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{
                Permission.MESSAGE_MANAGE
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 1 && args[0].matches("^([1-9][0-9]{0,2}|1000)$")) {
                    int toPurgeAmount = Integer.parseInt(args[0]);
                    MessageHistory messageHistory = event.getTextChannel().getHistory();
                    Collector<Message> collector = new Collector<>();
                    while (toPurgeAmount > 0) {
                        if (toPurgeAmount > 99) {
                            messageHistory.retrievePast(100).queue(collector::accept);
                            collector.increment();
                            toPurgeAmount -= 100;
                        } else {
                            messageHistory.retrievePast(toPurgeAmount + 1).queue(collector::accept);
                            collector.increment();
                            toPurgeAmount = 0;
                        }
                    }
                    collector.collect(collection -> {
                        int old = areNotDeletable(collection);
                        if (old > 0) {
                            event.reply("Max purge size of this chat is **" + (collection.size() - old) + "** because I can't bulk delete messages older then 2 weeks");
                        } else {
                            collection.forEach(message -> MessageHelper.purgedMessageDeleter.putIfAbsent(message.getIdLong(), event.getAuthorId()));
                            event.getTextChannel().purgeMessages(collection);
                            event.getTextChannel().sendMessage("**Done**").queue(m -> {
                                m.delete().queueAfter(3, TimeUnit.SECONDS);
                                MessageHelper.botDeletedMessages.add(m.getIdLong());
                            });
                        }
                    });
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }

    private int areNotDeletable(List<Message> toDelete) {
        int foundOldMessage = 0;
        if (toDelete.size() == 0 || OffsetDateTime.now().toEpochSecond() - toDelete.get(toDelete.size() - 1).getCreationTime().toEpochSecond() < 336 * 3600)
            return foundOldMessage;
        for (Message msg : toDelete) {
            if (OffsetDateTime.now().toEpochSecond() - msg.getCreationTime().toEpochSecond() > 336 * 3600) {
                foundOldMessage++;
            }
        }
        return foundOldMessage;
    }
}
