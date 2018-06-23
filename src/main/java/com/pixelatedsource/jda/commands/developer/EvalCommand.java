package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class EvalCommand extends Command {

    public EvalCommand() {
        this.commandName = "eval";
        this.description = "eval stuff";
        this.usage = PREFIX + commandName + " <engine> [insert crappy code]";
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        String toEval = event.getArgs().replaceFirst(args[0], "");
        ScriptEngine se = new ScriptEngineManager().getEngineByName(args[0]);
        if (se == null) {
            MessageHelper.sendUsage(this, event); return;
        }

        se.put("event", event);
        se.put("jda", event.getJDA());
        se.put("guild", event.getGuild());
        se.put("channel", event.getChannel());
        se.put("mysql", PixelSniper.mySQL);
        se.put("eb", new EmbedBuilder());
        if (event.getTextChannel().isNSFW())
            se.put("webUtils", WebUtils.getWebUtilsInstance());
        try {
            se.eval(toEval);
            event.getMessage().addReaction("\u2705").queue();
        } catch (Exception e) {
            event.getMessage().addReaction("\u274C").queue();
            event.reply("An exception was thrown:\n```\n" + e + "```");
        }
    }
}
