package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.Private;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class PotatoCommand extends Command {

    public PotatoCommand() {
        this.commandName = "potato";
        this.description = "Shows a potato";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.id = 43;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                event.reply(new Embedder(event.getGuild())
                        .setDescription("Enjoy your delicious \uD83E\uDD54")
                        .setImage(Private.getWeebV1Url("potato"))
                        .setFooter("Powered by weeb.sh", null)
                        .build());
            else
                event.reply("Enjoy your \uD83E\uDD54 \n" + Private.getWeebV1Url("potato"));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
