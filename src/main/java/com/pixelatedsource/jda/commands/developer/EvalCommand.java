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
import java.util.Arrays;
import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class EvalCommand extends Command {

    public EvalCommand() {
        this.commandName = "eval";
        this.description = "eval stuff";
        this.usage = PREFIX + commandName + " <engine> [insert crappy code]";
        this.category = Category.DEVELOPER;
    }

    public static EvalCommand INSTANCE = new EvalCommand();
    private List<Long> blackList = Arrays.asList(110373943822540800L, 264445053596991498L);

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
        se.put("webUtils", WebUtils.getWebUtilsInstance());
        se.put("blacklist", blackList);
        try {
            if (toEval.contains("event.reply("))
                se.eval(toEval);
            else
                event.reply(se.eval(toEval).toString());
            event.getMessage().addReaction("\u2705").queue();
        } catch (Exception e) {
            event.getMessage().addReaction("\u274C").queue();
            event.reply("An exception was thrown:\n```\n" + e + "```");
        }
    }

    public List<Long> getBlackList() {
        return blackList;
    }
}
