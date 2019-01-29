package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static me.melijn.jda.Melijn.PREFIX;

public class HistoryCommand extends Command {

    public HistoryCommand() {
        this.commandName = "history";
        this.description = "Shows bans/warns/mutes/kicks of a user";
        this.usage = PREFIX + commandName + " <bans | mutes | warns | kicks> <user>";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 47;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length < 2) {
                event.sendUsage(this, event);
                return;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                removeSection(event);
                return;
            }

            event.getHelpers().retrieveUserByArgsN(event, args[1], (success) -> {
                if (success == null) {
                    event.reply("Unknown user");
                    return;
                }
                switch (args[0]) {
                    case "ban":
                    case "bans":
                        event.async(() -> event.getMySQL().getUserBans(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), bans -> {
                            EmbedBuilder ebBan = new Embedder(event.getVariables(), event.getGuild());
                            getLongMessageInParts(new ArrayList<>(Arrays.asList(bans)), parts -> {
                                        int partnumber = 0;
                                        int size = Integer.parseInt(parts.get(parts.size() - 1));
                                        parts.remove(parts.size() - 1);
                                        for (String part : parts) {
                                            ebBan.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s bans " + ++partnumber + "/" + size + " ".repeat(50).substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                            ebBan.setDescription(part);
                                            event.reply(ebBan.build());
                                        }
                                    }
                            );
                        }));
                        break;
                    case "mute":
                    case "mutes":
                        event.async(() -> event.getMySQL().getUserMutes(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), mutes -> {
                            EmbedBuilder ebMute = new Embedder(event.getVariables(), event.getGuild());
                            getLongMessageInParts(new ArrayList<>(Arrays.asList(mutes)), parts -> {
                                        int partnumber = 0;
                                        int size = Integer.parseInt(parts.get(parts.size() - 1));
                                        parts.remove(parts.size() - 1);
                                        for (String part : parts) {
                                            ebMute.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s mutes " + ++partnumber + "/" + size + " ".repeat(50).substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                            ebMute.setDescription(part);
                                            event.reply(ebMute.build());
                                        }
                                    }
                            );
                        }));
                        break;
                    case "warn":
                    case "warns":
                        event.async(() -> event.getMySQL().getUserWarns(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), warns -> {
                            EmbedBuilder ebWarn = new Embedder(event.getVariables(), event.getGuild());
                            getLongMessageInParts(new ArrayList<>(Arrays.asList(warns)), parts -> {
                                        int partnumber = 0;
                                        int size = Integer.parseInt(parts.get(parts.size() - 1));
                                        parts.remove(parts.size() - 1);
                                        for (String part : parts) {
                                            ebWarn.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s warns " + ++partnumber + "/" + size + " ".repeat(50).substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                            ebWarn.setDescription(part);
                                            event.reply(ebWarn.build());
                                        }
                                    }
                            );
                        }));
                        break;
                    case "kick":
                    case "kicks":
                        event.async(() -> event.getMySQL().getUserKicks(event.getGuild().getIdLong(), success.getIdLong(), event.getJDA(), kicks -> {
                            EmbedBuilder ebKick = new Embedder(event.getVariables(), event.getGuild());
                            getLongMessageInParts(new ArrayList<>(Arrays.asList(kicks)), parts -> {
                                        int partnumber = 0;
                                        int size = Integer.parseInt(parts.get(parts.size() - 1));
                                        parts.remove(parts.size() - 1);
                                        for (String part : parts) {
                                            ebKick.setAuthor(success.getName() + "#" + success.getDiscriminator() + "'s kicks " + ++partnumber + "/" + size + " ".repeat(50).substring(0, 45 - success.getName().length()) + "\u200B", null, success.getEffectiveAvatarUrl());
                                            ebKick.setDescription(part);
                                            event.reply(ebKick.build());
                                        }
                                    }
                            );
                        }));
                        break;
                    default:
                        event.sendUsage(this, event);
                        break;
                }
            });
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void getLongMessageInParts(List<String> stuff, Consumer<List<String>> callback) {
        StringBuilder part = new StringBuilder();
        List<String> parts = new ArrayList<>();
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
        if (part.length() > 0) parts.add(part.toString());
        parts.add(String.valueOf(count));
        callback.accept(parts);
    }

    private void removeSection(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        Guild guild = event.getGuild();
        if (args.length < 4) {
            event.reply("Usage: " + event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " remove <ban | warn | kick | mute> <user> <moment>");
            return;
        }

        User user = event.getHelpers().getUserByArgs(event, args[2]);
        if (user == null || guild.getMember(user) == null) {
            event.reply("Unknown member");
            return;
        }

        long millis = event.getMessageHelper().dateToMillis(event.getArgs().replaceFirst("\\s+" + args[0] + "\\s+" + args[1] + "\\s+", ""));

        if (millis == -1) {
            event.reply("Wrong time format it should be: `h:m:s`s `d:M:yyyy`");
            return;
        }

        switch (args[1]) {
            case "ban":
                event.getMySQL().removeBan(guild.getMember(user), millis);
                event.reply("Successfully removed the ban");
                break;
            case "mute":
                event.getMySQL().removeMute(guild.getMember(user), millis);
                event.reply("Successfully removed the mute");
                break;
            case "warn":
                event.getMySQL().removeWarn(guild.getMember(user), millis);
                event.reply("Successfully removed the warn");
                break;
            case "kick":
                event.getMySQL().removeKick(guild.getMember(user), millis);
                event.reply("Successfully removed the kick");
                break;
            default:
                event.reply("Usage: " + event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " remove <ban | warn | kick | mute> <user> <moment>");
                break;
        }
    }
}
