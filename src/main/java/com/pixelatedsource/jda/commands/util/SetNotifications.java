package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.NotificationType;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;
import static com.pixelatedsource.jda.PixelSniper.mySQL;


public class SetNotifications extends Command {

    public SetNotifications() {
        this.commandName = "SetNotifications";
        this.description = "Get notified for certain events";
        this.usage = PREFIX + commandName + " [nextVote] [user/info]";
        this.extra = "arg1 -> notification type\narg2 -> either view all notifications of the type or toggle a user on and off";
        this.category = Category.UTILS;
    }

    public static HashMap<Long, ArrayList<Long>> nextVotes = mySQL.getNotificationsMap(NotificationType.NEXTVOTE);

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
            switch (args[0].toLowerCase()) {
                case "nextvote": {
                    switch (args.length) {
                        case 1: {
                            ArrayList<Long> list = nextVotes.getOrDefault(event.getAuthorId(), new ArrayList<>());
                            StringBuilder contentBuilder = new StringBuilder();
                            contentBuilder.append("```INI\n");
                            int i = 0;
                            if (list.size() != 0) {
                                for (long s : list) {
                                    User user = event.getJDA().retrieveUserById(s).complete();
                                    if (user != null) {
                                        contentBuilder.append(++i).append(". ").append("[").append(user.getName()).append("#").append(user.getDiscriminator()).append("]\n");
                                    }
                                }
                                if (i == 0) contentBuilder.append(";none");
                            }
                            contentBuilder.append("```");
                            event.reply("**" + event.getFullAuthorName() + "'s** nextVote notifications\n" + contentBuilder.toString());
                            break;
                        }
                        case 2: {
                            if (args[1].equalsIgnoreCase("info")) {
                                ArrayList<Long> list = nextVotes.getOrDefault(event.getAuthorId(), new ArrayList<>());
                                StringBuilder contentBuilder = new StringBuilder();
                                contentBuilder.append("```INI\n");
                                int i = 0;
                                if (list.size() != 0) {
                                    for (long s : list) {
                                        User user = event.getJDA().retrieveUserById(s).complete();
                                        if (user != null) {
                                            contentBuilder.append(++i).append(". ").append("[").append(user.getName()).append("#").append(user.getDiscriminator()).append("]\n");
                                        }
                                    }
                                    if (i == 0) contentBuilder.append(";none");
                                }
                                contentBuilder.append("```");
                                event.reply("**" + event.getFullAuthorName() + "'s** nextVote notifications\n" + contentBuilder.toString());
                                break;
                            } else {
                                User user; //Yes this has to be 3 lines because of lambda's :(
                                user = Helpers.getUserByArgs(event, args[1]);
                                boolean contains = false;
                                for (long s : nextVotes.getOrDefault(user.getId(), new ArrayList<>())) {
                                    if (!contains && user.getIdLong() == s) {
                                        contains = true;
                                    }
                                }
                                if (contains) {
                                    ArrayList<Long> list = nextVotes.get(event.getAuthorId());
                                    list.remove(user.getIdLong());
                                    nextVotes.replace(event.getAuthorId(), list);
                                    new Thread(() -> {
                                        mySQL.removeNotification(event.getAuthorId(), user.getIdLong(), NotificationType.NEXTVOTE);
                                        event.reply("NextVote notifications for **" + user.getName() + "#" + user.getDiscriminator() + "** have been **disabled**");
                                    }).start();
                                } else {
                                    ArrayList<Long> list = nextVotes.getOrDefault(event.getAuthorId(), new ArrayList<>());
                                    list.add(user.getIdLong());
                                    if (nextVotes.get(event.getAuthorId()) != null)
                                        nextVotes.replace(event.getAuthorId(), list);
                                    else nextVotes.put(event.getAuthorId(), list);
                                    new Thread(() -> {
                                        mySQL.putNotifcation(event.getAuthorId(), user.getIdLong(), NotificationType.NEXTVOTE);
                                        event.reply("NextVote notifications for **" + user.getName() + "#" + user.getDiscriminator() + "** have been **enabled**");
                                    }).start();
                                }
                            }
                            break;
                        }
                    }
                }
                break;
                default:
                    break;
            }
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}