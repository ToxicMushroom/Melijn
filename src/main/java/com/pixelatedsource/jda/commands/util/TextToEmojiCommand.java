package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TextToEmojiCommand extends Command {

    public TextToEmojiCommand() {
        this.commandName = "t2e";
        this.description = "Converts input text and numbers to emotes";
        this.usage = PREFIX + this.commandName + " [%spaces%] <text>";
        this.aliases = new String[]{"TextToEmojis"};
        this.extra = "%spaces% will put a space after each emoji so they don't change into flags when copied and pasted";
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getArgs().length() > 0) {
                StringBuilder sb = new StringBuilder();
                String[] args = event.getArgs().split("\\s+");
                String text = event.getArgs();
                String pattern1 = "([a-z])|([A-Z])|([0-9])";
                Pattern r1 = Pattern.compile(pattern1);
                Matcher m1 = r1.matcher(text);
                if (m1.find()) {
                    Logger.getLogger(this.getClass().getName()).info("groupcount: " + m1.groupCount());
                    Logger.getLogger(this.getClass().getName()).info("groupcount: " + m1.group(1));
                }


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
                    if (sb.toString().length() > 1900)  {
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
