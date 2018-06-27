package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LoopQueueCommand extends Command {

    public LoopQueueCommand() {
        this.commandName = "loopQueue";
        this.description = "Change the looping state or view the looping state of the queue";
        this.usage = PREFIX + this.commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeatq", "loopq"};
        this.category = Category.MUSIC;
    }

    public static HashMap<Long, Boolean> looped = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        LoopCommand.executorLoops(this, event, looped);
    }
}
