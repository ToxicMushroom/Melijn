package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PotatoCommand extends Command {

    public PotatoCommand() {
        this.commandName = "potato";
        this.description = "shows you a delicious treat";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"aardappel", "patat"};
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("Enjoy your delicious \uD83E\uDD54")
                        .setImage(WebUtils.getUrl("potato"))
                        .build());
            else
                event.reply("Enjoy your \uD83E\uDD54 \n" + WebUtils.getUrl("potato"));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
