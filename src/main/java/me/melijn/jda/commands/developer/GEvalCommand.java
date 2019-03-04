package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static me.melijn.jda.Melijn.PREFIX;

public class GEvalCommand extends Command {

    public GEvalCommand() {
        this.commandName = "gEval";
        this.description = "evaluates groovy code";
        this.usage = PREFIX + commandName + " [crappy groovy code]";
        this.aliases = new String[]{"groovyEval"};
        this.category = Category.DEVELOPER;
        this.id = 113;
    }

    @Override
    protected void execute(CommandEvent event) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        event.getHelpers().eval(event, engine);
    }
}
