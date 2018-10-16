package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class SetEvalEngineCommand extends Command {

    public SetEvalEngineCommand() {
        this.commandName = "setevalengine";
        this.description = "Set the eval engine of >eval";
        this.usage = PREFIX + commandName + " [engine]";
        this.aliases = new String[]{"see"};
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0 && !args[0].isBlank()) {
            EvalCommand.INSTANCE.engineName = event.getArgs();
            event.reply("Changed eval engine to: **" + EvalCommand.INSTANCE.engineName + "**");
        } else {
            event.reply("EvalEngine: **" + EvalCommand.INSTANCE.engineName + "**");
        }
    }
}
