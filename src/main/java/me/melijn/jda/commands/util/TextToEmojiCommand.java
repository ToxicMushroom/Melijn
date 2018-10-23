package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;

import static me.melijn.jda.Melijn.PREFIX;

public class TextToEmojiCommand extends Command {

    public TextToEmojiCommand() {
        this.commandName = "t2e";
        this.description = "Converts input text and numbers to emotes";
        this.usage = PREFIX + commandName + " [%spaces%] <text>";
        this.aliases = new String[]{"TextToEmojis"};
        this.extra = "%spaces% will put a space after each emoji so they don't change into flags when copied and pasted";
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (event.getArgs().length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (String s : event.getArgs().replaceFirst("%spaces%", "").split("")) {
                    if (Character.isLetter(s.toLowerCase().charAt(0))) {
                        sb.append(":regional_indicator_").append(s.toLowerCase()).append(":");
                        if (args[0].equalsIgnoreCase("%spaces%")) {
                            sb.append(" ");
                        }
                    } else if (Character.isDigit(s.charAt(0))) {
                        sb.append(":").append(Helpers.numberToString(Integer.valueOf(s))).append(":");
                        if (args[0].equalsIgnoreCase("%spaces%")) {
                            sb.append(" ");
                        }
                    } else {
                        sb.append(s);
                    }
                    if (sb.length() > 1900)  {
                        event.reply(sb.toString());
                        sb = new StringBuilder();
                    }
                }
                event.reply(new EmbedBuilder().setDescription(sb.toString()).setColor(Helpers.EmbedColor).build());
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
