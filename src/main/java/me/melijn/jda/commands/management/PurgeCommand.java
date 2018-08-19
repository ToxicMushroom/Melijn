package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static me.melijn.jda.Melijn.PREFIX;

public class PurgeCommand extends Command {

    public PurgeCommand() {
        this.commandName = "purge";
        this.description = "Deletes messages";
        this.usage = PREFIX + commandName + " [1 - 500]";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{
                Permission.MESSAGE_MANAGE
        };
    }

    private Executor service = Executors.newFixedThreadPool(5);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 1 && args[0].matches("\\d+")) {
                    try {
                        service.execute(() -> {
                            int initAmount = Integer.parseInt(args[0]);
                            int toPurgeAmount = initAmount;
                            if (toPurgeAmount < 1) {
                                MessageHelper.sendUsage(this, event);
                                return;
                            }
                            MessageHistory messageHistory = event.getTextChannel().getHistory();
                            List<Message> toPurge = new ArrayList<>();
                            while (toPurgeAmount >= 100) {
                                List<Message> buffer = messageHistory.retrievePast(100).complete();
                                if ((buffer.size() > 0) && OffsetDateTime.now().toEpochSecond() - buffer.get(buffer.size() - 1).getCreationTime().toEpochSecond() > 336 * 3600) {
                                    int youngMessages = initAmount - toPurgeAmount;
                                    for (Message msg : buffer) {
                                        if (OffsetDateTime.now().toEpochSecond() - msg.getCreationTime().toEpochSecond() < 336 * 3600)
                                            youngMessages++;
                                    }
                                    event.reply("Max purge size of this chat is **" + ++youngMessages + "** because I can't bulk delete messages older then 2 weeks");
                                    return;
                                }
                                toPurge.addAll(buffer);
                                toPurgeAmount -= 100;
                            }
                            List<Message> buffer = messageHistory.retrievePast(toPurgeAmount + 1).complete();
                            if ((buffer.size() > 0) && OffsetDateTime.now().toEpochSecond() - buffer.get(buffer.size() - 1).getCreationTime().toEpochSecond() > 336 * 3600) {
                                int youngMessages = initAmount - toPurgeAmount;
                                for (Message msg : buffer) {
                                    if (OffsetDateTime.now().toEpochSecond() - msg.getCreationTime().toEpochSecond() < 336 * 3600)
                                        youngMessages++;
                                }
                                event.reply("Max purge size of this chat is **" + (youngMessages + 1) + "** because I can't bulk delete messages older then 2 weeks");
                                return;
                            }
                            toPurge.addAll(buffer);
                            LinkedHashSet<Message> toPurgeSet = new LinkedHashSet<>(toPurge);
                            toPurge.clear();
                            toPurge.addAll(toPurgeSet);
                            toPurgeSet.clear();
                            toPurge.forEach(blub -> MessageHelper.purgedMessages.put(blub.getIdLong(), event.getAuthorId()));
                            while (toPurge.size() > 100) {
                                List<Message> deleteableMessages = new ArrayList<>();
                                while (deleteableMessages.size() != 100) {
                                    deleteableMessages.add(toPurge.get(deleteableMessages.size()));
                                }
                                toPurge.removeAll(deleteableMessages);
                                event.getTextChannel().deleteMessages(deleteableMessages).queue();
                            }
                            if (toPurge.size() == 1) event.getTextChannel().deleteMessageById(toPurge.get(0).getIdLong()).queue();
                            else if (toPurge.size() > 1) event.getTextChannel().deleteMessages(toPurge).queue();

                        });
                    } catch (NumberFormatException e) {
                        MessageHelper.sendUsage(this, event);
                    }
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
}
