package me.melijn.jda.commands.music;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class LoopQueueCommand extends Command {



    public LoopQueueCommand() {
        this.commandName = "loopQueue";
        this.description = "Changes the looping state of entire the queue";
        this.usage = PREFIX + commandName + " [false/off/yes | true/on/off]";
        this.extra = "When a track finishes playing or gets skipped it will move to the bottom of the queue";
        this.aliases = new String[]{"repeatq", "loopq"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 31;
    }

    @Override
    protected void execute(CommandEvent event) {
        LoopCommand.executorLoops(this, event, event.getVariables().loopedQueues);
    }
}
