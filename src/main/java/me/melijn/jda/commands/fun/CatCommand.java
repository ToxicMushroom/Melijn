package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class CatCommand extends Command {

    public CatCommand() {
        this.commandName = "cat";
        this.description = "Shows you a random cat";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"kitten", "kat", "poes"};
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String url = webUtils.getCatUrl();
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                if (url != null)
                    event.reply(new EmbedBuilder()
                            .setColor(Helpers.EmbedColor)
                            .setDescription("Enjoy your \uD83D\uDC31 ~meow~")
                            .setImage(url)
                            .build());
                else {
                    webUtils.getImage("animal_cat", image -> MessageHelper.sendFunText("Enjoy your \uD83D\uDC31 ~meow~", image.getUrl(), event));
                }
            else
                event.reply("Enjoy your \uD83D\uDC31 ~meow~\n" + url);
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
