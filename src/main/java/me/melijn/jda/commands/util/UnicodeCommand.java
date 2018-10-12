package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;

public class UnicodeCommand extends Command {

    public UnicodeCommand() {
        this.commandName = "unicode";
        this.description = "Converts an input to unicode";
        this.usage = PREFIX + commandName + " <input>";
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String arg = event.getArgs();
            if (arg.length() <= 0) {
                MessageHelper.sendUsage(this, event);
                return;
            }
            if (arg.matches("<:.*:\\d+>")) {
                String id = arg.replaceAll("<:.*:(\\d+)>", "$1");
                String name = arg.replaceAll("<:(.*):\\d+>", "$1");
                event.reply(
                        "Name: **" + name + "**\n" +
                                "ID: **" + id + "**\n" +
                                "URL: **https://discordcdn.com/emojis/" + id + ".png?size=2048**");
                return;
            }
            StringBuilder builder = new StringBuilder();
            arg.codePoints().forEachOrdered(code -> {
                char[] chars = Character.toChars(code);
                if (chars.length > 1) {
                    StringBuilder hex0 = new StringBuilder(Integer.toHexString(chars[0]).toUpperCase());
                    StringBuilder hex1 = new StringBuilder(Integer.toHexString(chars[1]).toUpperCase());
                    while (hex0.length() < 4)
                        hex0.insert(0, "0");
                    while (hex1.length() < 4)
                        hex1.insert(0, "0");
                    builder.append("`\\u").append(hex0).append("\\u").append(hex1).append("`   ");
                } else {
                    StringBuilder hex = new StringBuilder(Integer.toHexString(code).toUpperCase());
                    while (hex.length() < 4)
                        hex.insert(0, "0");
                    builder.append("\n`\\u").append(hex).append("`   ");
                }
                builder.append(String.valueOf(chars)).append("   _").append(Character.getName(code)).append("_");
            });
            MessageHelper.sendSplitMessage(event.getTextChannel(), builder.toString());
        }
    }
}
