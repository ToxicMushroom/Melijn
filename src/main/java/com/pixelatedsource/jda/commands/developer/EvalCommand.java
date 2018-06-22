package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;

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
        se.put("event", event);
        se.put("jda", event.getJDA());
        se.put("guild", event.getGuild());
        se.put("channel", event.getChannel());
        if (event.getTextChannel().isNSFW())
            se.put("webUtils", WebUtils.getWebUtilsInstance());
        try {
            event.reply("Evaluated Successfully:\n```\n" + se.eval(toEval) + "```");
        } catch (Exception e) {
            event.reply("An exception was thrown:\n```\n" + e + "```");
        }
    }
}
