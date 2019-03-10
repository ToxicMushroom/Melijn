package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static me.melijn.jda.Melijn.PREFIX;

public class JSEvalCommand extends Command {

    public JSEvalCommand() {
        this.commandName = "jsEval";
        this.description = "evaluates javascript code";
        this.usage = PREFIX + commandName + " [crappy javascript code]";
        this.aliases = new String[]{"javascriptEval"};
        this.category = Category.DEVELOPER;
        this.id = 114;
    }

    @Override
    protected void execute(CommandEvent event) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        event.getHelpers().eval(event, engine, "js");
    }
}
