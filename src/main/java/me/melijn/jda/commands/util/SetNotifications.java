package me.melijn.jda.commands.util;

import gnu.trove.list.TLongList;
import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.NotificationType;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.User;

import java.util.concurrent.atomic.AtomicInteger;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.Melijn.mySQL;


public class SetNotifications extends Command {

    public SetNotifications() {
        this.commandName = "setNotifications";
        this.description = "configures vote notification events";
        this.usage = PREFIX + commandName + " [nextVote] [user/info]";
        this.extra = "arg1 -> notification type\narg2 -> either view all notifications of the type or toggle a user on and off";
        this.category = Category.UTILS;
        this.aliases = new String[]{"sn"};
        this.id = 32;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0 && !args[0].isEmpty()) {
            if ("nextvote".equals(args[0].toLowerCase())) {
                if (args.length == 1 || args[1].equalsIgnoreCase("info")) {
                    TLongList list = mySQL.getNotifications(event.getAuthorId(), NotificationType.NEXTVOTE);
                    final StringBuilder contentBuilder = new StringBuilder("```INI\n");
                    AtomicInteger progress = new AtomicInteger(0);
                    AtomicInteger contentRowsCount = new AtomicInteger(0);
                    if (list.size() != 0) {
                        for (long s : list.toArray()) {
                            event.getJDA().retrieveUserById(s).queue(user -> {
                                progress.set(progress.get() + 1);
                                if (user != null)
                                    contentBuilder.append(contentRowsCount.incrementAndGet() + 1).append(". ").append("[").append(user.getName()).append("#").append(user.getDiscriminator()).append("]\n");
                                if (progress.get() == list.size()) {
                                    if (contentRowsCount.get() == 0) contentBuilder.append(";none");
                                    contentBuilder.append("```");
                                    event.reply("**" + event.getFullAuthorName() + "'s** nextVote notifications\n" + contentBuilder.toString());
                                }
                            });
                        }
                    }
                } else {
                    User user = Helpers.getUserByArgs(event, args[1]);
                    TLongList list = mySQL.getNotifications(user.getIdLong(), NotificationType.NEXTVOTE);
                    boolean shouldRemove = false;
                    for (long s : list.toArray()) {
                        if (!shouldRemove && user.getIdLong() == s) {
                            shouldRemove = true;
                        }
                    }
                    if (shouldRemove) {
                        TaskScheduler.async(() -> {
                            mySQL.removeNotification(event.getAuthorId(), user.getIdLong(), NotificationType.NEXTVOTE);
                            event.reply("NextVote notifications for **" + user.getName() + "#" + user.getDiscriminator() + "** have been **disabled**");
                        });
                    } else {
                        TaskScheduler.async(() -> {
                            mySQL.putNotification(event.getAuthorId(), user.getIdLong(), NotificationType.NEXTVOTE);
                            event.reply("NextVote notifications for **" + user.getName() + "#" + user.getDiscriminator() + "** have been **enabled**");
                        });
                    }
                }
            }
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}