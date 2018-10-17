package me.melijn.jda.commands.developer;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static me.melijn.jda.Melijn.PREFIX;

public class EvalCommand extends Command {

    public static EvalCommand INSTANCE = new EvalCommand();
    public static TLongList serverBlackList = new TLongArrayList();
    public static TLongList userBlackList = new TLongArrayList();
    String engineName = "groovy";

    public EvalCommand() {
        this.commandName = "eval";
        this.description = "eval stuff";
        this.usage = PREFIX + commandName + " [insert crappy code]";
        this.category = Category.DEVELOPER;
    }

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

        String imports = "import java.io.*;\n" +
                "import java.lang.*;\n" +
                "import java.util.*;\n" +
                "import net.dv8tion.jda.core.*;\n" +
                "import me.melijn.jda.blub.*;\n" +
                "import me.melijn.jda.commands.developer.*;\n" +
                "import me.melijn.jda.commands.management.*;\n" +
                "import me.melijn.jda.commands.music.*;\n" +
                "import me.melijn.jda.commands.util.*;\n" +
                "import me.melijn.jda.Melijn;\n" +
                "import me.melijn.jda.Helpers;\n";


        TaskScheduler.async(() -> {
            se.put("event", event);
            se.put("jda", event.getJDA());
            se.put("guild", event.getGuild());
            se.put("channel", event.getChannel());
            se.put("message", event.getMessage());
            se.put("mysql", Melijn.mySQL);
            se.put("eb", new EmbedBuilder());
            se.put("webUtils", WebUtils.getWebUtilsInstance());
            se.put("serverBlackList", serverBlackList);
            se.put("userBlackList", userBlackList);
            se.put("voteReq", Helpers.voteChecks);

            try {
                if (event.getArgs().contains("event.reply("))
                    se.eval(imports + event.getArgs());
                else
                    event.reply(se.eval(imports + event.getArgs()).toString());
            } catch (Exception e) {
                event.reply("An exception was thrown:\n```\n" + e + "```");
            }
        });
    }
}
