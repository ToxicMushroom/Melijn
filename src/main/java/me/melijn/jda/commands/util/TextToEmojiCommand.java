package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;

import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class TextToEmojiCommand extends Command {

    private static final List<String> numbers = Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine");

    public TextToEmojiCommand() {
        this.commandName = "t2e";
        this.description = "Converts input text and numbers to emotes";
        this.usage = PREFIX + commandName + " [%spaces%] <text>";
        this.aliases = new String[]{"TextToEmojis"};
        this.extra = "%spaces% will put a space after each emoji so they don't change into flags when copied and pasted";
        this.category = Category.UTILS;
        this.id = 69;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (event.getArgs().isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (char c : event.getArgs().replaceFirst("%spaces%", "").toCharArray()) {
                if (Character.isLetter(c)) {
                    sb.append(":regional_indicator_").append(Character.toLowerCase(c)).append(":");
                    if (args[0].equalsIgnoreCase("%spaces%")) {
                        sb.append(" ");
                    }
                } else if (Character.isDigit(c)) {
                    sb.append(":").append(numbers.get(Character.getNumericValue(c))).append(":");
                    if (args[0].equalsIgnoreCase("%spaces%")) {
                        sb.append(" ");
                    }
                } else {
                    sb.append(c);
                }
                if (sb.length() > 1900) {
                    event.reply(sb.toString());
                    sb = new StringBuilder();
                }
            }
            event.reply(new Embedder(event.getVariables(), event.getGuild()).setDescription(sb.toString()).build());

        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
