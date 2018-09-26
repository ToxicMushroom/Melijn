package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static me.melijn.jda.utils.MessageHelper.spaces;

public class HistoryCommand extends Command {

    public HistoryCommand() {
        this.commandName = "history";
        this.description = "View bans/warns/mutes/kicks of a user.";
        this.usage = Melijn.PREFIX + commandName + " <bans | mutes | warns | kicks> <user>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 2) {
                    Helpers.retrieveUserByArgsN(event, args[1], (success) -> {
                        if (success != null) {
                            switch (args[0]) {
                                case "ban":
                                case "bans":
                                    TaskScheduler.async(() -> Melijn.mySQL.getUserBans(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), bans -> {
                                        EmbedBuilder ebBan = new EmbedBuilder();
                                        getLongMessageInParts(new ArrayList<>(Arrays.asList(bans)), parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebBan.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s bans " + ++partnumber + "/" + size + spaces.substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                                        ebBan.setDescription(part);
                                                        ebBan.setColor(Helpers.EmbedColor);
                                                        event.reply(ebBan.build());
                                                    }
                                                }
                                        );
                                    }));
                                    break;
                                case "mute":
                                case "mutes":
                                    TaskScheduler.async(() -> Melijn.mySQL.getUserMutes(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), mutes -> {
                                        EmbedBuilder ebMute = new EmbedBuilder();
                                        getLongMessageInParts(new ArrayList<>(Arrays.asList(mutes)), parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebMute.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s mutes " + ++partnumber + "/" + size + spaces.substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                                        ebMute.setDescription(part);
                                                        ebMute.setColor(Helpers.EmbedColor);
                                                        event.reply(ebMute.build());
                                                    }
                                                }
                                        );
                                    }));
                                    break;
                                case "warn":
                                case "warns":
                                    TaskScheduler.async(() -> Melijn.mySQL.getUserWarns(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), warns -> {
                                        EmbedBuilder ebWarn = new EmbedBuilder();
                                        getLongMessageInParts(new ArrayList<>(Arrays.asList(warns)), parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebWarn.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s warns " + ++partnumber + "/" + size + spaces.substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                                        ebWarn.setDescription(part);
                                                        ebWarn.setColor(Helpers.EmbedColor);
                                                        event.reply(ebWarn.build());
                                                    }
                                                }
                                        );
                                    }));
                                    break;
                                case "kick":
                                case "kicks":
                                    TaskScheduler.async(() -> Melijn.mySQL.getUserKicks(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), kicks -> {
                                        EmbedBuilder ebKick = new EmbedBuilder();
                                        getLongMessageInParts(new ArrayList<>(Arrays.asList(kicks)), parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebKick.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s kicks " + ++partnumber + "/" + size + spaces.substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                                        ebKick.setDescription(part);
                                                        ebKick.setColor(Helpers.EmbedColor);
                                                        event.reply(ebKick.build());
                                                    }
                                                }
                                        );
                                    }));
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    break;
                            }
                        } else {
                            event.reply("Unknown user");
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

    private void getLongMessageInParts(ArrayList<String> stuff, Consumer<List<String>> callback) {
        StringBuilder part = new StringBuilder();
        ArrayList<String> parts = new ArrayList<>();
        int count = 1;
        for (String stuffdeel : stuff) {
            if (part.length() + stuffdeel.length() < 1900) {
                part.append(stuffdeel);
            } else if (part.length() > 0) {
                count++;
                parts.add(part.toString());
                part = new StringBuilder();
                part.append(stuffdeel);
            } else {
                part.append(".");
            }
        }
        if (part.toString().length() > 0) parts.add(part.toString());
        parts.add(String.valueOf(count));
        callback.accept(parts);
    }
}
