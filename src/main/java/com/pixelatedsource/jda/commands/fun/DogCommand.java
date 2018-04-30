package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DogCommand extends Command {

    public DogCommand() {
        this.commandName = "dog";
        this.description = "Shows you a random dog";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"hond"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("Enjoy your \uD83D\uDC36 ~woof~")
                        .setImage(WebUtils.getDogUrl())
                        .build());
            else
                event.reply("Enjoy your \uD83D\uDC36 ~woof~\n" + WebUtils.getDogUrl());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
