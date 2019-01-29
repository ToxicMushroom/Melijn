package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class CatCommand extends Command {


    public CatCommand() {
        this.commandName = "cat";
        this.description = "Shows you a cat";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"kitten", "kat", "poes"};
        this.category = Category.FUN;
        this.id = 60;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String url = event.getWebUtils().getCatUrl();
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                if (url != null)
                    event.reply(new Embedder(event.getVariables(), event.getGuild())
                            .setDescription("Enjoy your \uD83D\uDC31 ~meow~")
                            .setImage(url)
                            .build());
                else {
                    event.getWebUtils().getImage("animal_cat", image -> event.getMessageHelper().sendFunText("Enjoy your \uD83D\uDC31 ~meow~", image.getUrl(), event));
                }
            else
                event.reply("Enjoy your \uD83D\uDC31 ~meow~\n" + url);
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
