package me.melijn.jda.commands.developer;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class EvalCommand extends Command {

    public EvalCommand() {
        this.commandName = "eval";
        this.description = "eval stuff";
        this.usage = PREFIX + commandName + " [insert crappy code]";
        this.category = Category.DEVELOPER;
    }

    public static EvalCommand INSTANCE = new EvalCommand();
    private List<Long> blackList = Arrays.asList(110373943822540800L, 264445053596991498L);
    String engineName = "groovy";

    @Override
    protected void execute(CommandEvent event) {
        if (engineName.equalsIgnoreCase("groovy")) {
            GroovyScriptEngineImpl se = (GroovyScriptEngineImpl) new ScriptEngineManager().getEngineByName("groovy");
            evaluate(event, se);
        } else {
            ScriptEngine se = new ScriptEngineManager().getEngineByName(engineName);
            evaluate(event, se);
        }
    }

    private void evaluate(CommandEvent event, ScriptEngine se) {
        if (se == null) {
            MessageHelper.sendUsage(this, event);
            return;
        }

        se.put("event", event);
        se.put("jda", event.getJDA());
        se.put("guild", event.getGuild());
        se.put("channel", event.getChannel());
        se.put("mysql", Melijn.mySQL);
        se.put("eb", new EmbedBuilder());
        se.put("webUtils", WebUtils.getWebUtilsInstance());
        se.put("blacklist", blackList);
        se.put("voteReq", Helpers.voteChecks);
        try {
            if (event.getArgs().contains("event.reply("))
                se.eval(event.getArgs());
            else
                event.reply(se.eval(event.getArgs()).toString());
        } catch (Exception e) {
            event.reply("An exception was thrown:\n```\n" + e + "```");
        }
    }

    public List<Long> getBlackList() {
        return blackList;
    }
}
