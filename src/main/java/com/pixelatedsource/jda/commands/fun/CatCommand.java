package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class CatCommand extends Command {

    public CatCommand() {
        this.commandName = "cat";
        this.description = "Shows you a random kitty";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"kitten", "kat", "poes"};
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                if (WebUtils.getCatUrl() != null)
                    event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("Enjoy your \uD83D\uDC31 ~meow~")
                        .setImage(WebUtils.getCatUrl())
                        .build());
                else event.reply("Cat failed to jump into my downloads folder :|\n We need to wait for more cats (402 forbidden)");
            else
                event.reply("Enjoy your \uD83D\uDC31 ~meow~\n" + WebUtils.getCatUrl());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
