package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HistoryCommand extends Command {

    public HistoryCommand() {
        this.commandName = "history";
        this.description = "View bans/warns/mutes/kicks of a user.";
        this.usage = PREFIX + commandName + " <bans | mutes | warns | kicks> <user>";
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
                                    new Thread(() -> {
                                        ArrayList<String> bans = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserBans(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA())));
                                        EmbedBuilder ebBan = new EmbedBuilder();
                                        getLongMessageInParts(bans, parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebBan.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s bans " + ++partnumber + "/" + size, null, success.getEffectiveAvatarUrl());
                                                        ebBan.setDescription(part);
                                                        ebBan.setColor(Helpers.EmbedColor);
                                                        event.reply(ebBan.build());
                                                    }
                                                }
                                        );
                                    }).start();
                                    break;
                                case "mute":
                                case "mutes":
                                    new Thread(() -> {
                                        ArrayList<String> mutes = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserMutes(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA())));
                                        EmbedBuilder ebMute = new EmbedBuilder();
                                        getLongMessageInParts(mutes, parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebMute.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s mutes " + ++partnumber + "/" + size, null, success.getEffectiveAvatarUrl());
                                                        ebMute.setDescription(part);
                                                        ebMute.setColor(Helpers.EmbedColor);
                                                        event.reply(ebMute.build());
                                                    }
                                                }
                                        );
                                    }).start();
                                    break;
                                case "warn":
                                case "warns":
                                    new Thread(() -> {
                                        ArrayList<String> warns = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserWarns(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA())));
                                        EmbedBuilder ebWarn = new EmbedBuilder();
                                        getLongMessageInParts(warns, parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebWarn.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s warns " + ++partnumber + "/" + size, null, success.getEffectiveAvatarUrl());
                                                        ebWarn.setDescription(part);
                                                        ebWarn.setColor(Helpers.EmbedColor);
                                                        event.reply(ebWarn.build());
                                                    }
                                                }
                                        );
                                    }).start();
                                    break;
                                case "kick":
                                case "kicks":
                                    new Thread(() -> {
                                        ArrayList<String> kicks = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserKicks(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA())));
                                        EmbedBuilder ebKick = new EmbedBuilder();
                                        getLongMessageInParts(kicks, parts -> {
                                                    int partnumber = 0;
                                                    int size = Integer.parseInt(parts.get(parts.size() - 1));
                                                    parts.remove(parts.size() - 1);
                                                    for (String part : parts) {
                                                        ebKick.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s kicks " + ++partnumber + "/" + size, null, success.getEffectiveAvatarUrl());
                                                        ebKick.setDescription(part);
                                                        ebKick.setColor(Helpers.EmbedColor);
                                                        event.reply(ebKick.build());
                                                    }
                                                }
                                        );
                                    }).start();
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
            }
        }
        if (part.toString().length() > 0) parts.add(part.toString());
        parts.add(String.valueOf(count));
        callback.accept(parts);
    }
}
