package me.melijn.jda.commands.music;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class LoopQueueCommand extends Command {

    public static TLongList looped = new TLongArrayList();

    public LoopQueueCommand() {
        this.commandName = "loopQueue";
        this.description = "Change the looping state or view the looping state of the queue";
        this.usage = PREFIX + commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeatq", "loopq"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        LoopCommand.executorLoops(this, event, looped);
    }
}
