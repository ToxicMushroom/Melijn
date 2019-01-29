package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class AlpacaCommand extends Command {

    public AlpacaCommand() {
        this.commandName = "alpaca";
        this.description = "Shows an alpaca";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.id = 93;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                event.reply(new Embedder(event.getVariables(), event.getGuild())
                        .setDescription("Enjoy your alpaca!")
                        .setImage(String.format("http://randomalpaca.com/wp-content/uploads/2015/04/alpaca%d.jpg",
                                event.getMessageHelper().randInt(1, 144)))
                        .build());
            else
                event.reply(String.format("Enjoy your alpaca\nhttp://randomalpaca.com/wp-content/uploads/2015/04/alpaca%d.jpg",
                        event.getMessageHelper().randInt(1, 144)));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
