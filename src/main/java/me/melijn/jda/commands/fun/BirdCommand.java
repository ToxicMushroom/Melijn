package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class BirdCommand extends Command {

    private WebUtils webUtils;

    public BirdCommand() {
        this.commandName = "bird";
        this.description = "Shows you a random bird";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"vogel"};
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String url = webUtils.getBirdUrl();
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                if (url != null)
                    event.reply(new Embedder(event.getGuild())
                            .setDescription("Enjoy your \uD83D\uDC26 ~tweet~")
                            .setImage(url)
                            .build());
                else {
                    event.reply("random.birb.pw is down :/");
                }
            else
                event.reply("Enjoy your \uD83D\uDC31 ~meow~\n" + url);
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
