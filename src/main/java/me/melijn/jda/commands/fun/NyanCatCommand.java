package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class NyanCatCommand extends Command {

    public NyanCatCommand() {
        this.commandName = "nyancat";
        this.description = "Shows a nyancat";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"nyan"};
        this.id = 11;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), commandName, 0)) {
            String emoji = (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EXT_EMOJI)) ?
                    "<:normie_nyan:490976816018751503>" :
                    "nyan";
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                event.reply(new Embedder(event.getGuild())
                        .setDescription("Enjoy your " + emoji + " ~meow!~")
                        .setImage("https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif")
                        .build());
            else
                event.reply("Enjoy your " + emoji + " ~meow!~\n"
                        + "https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
