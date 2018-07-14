package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.pixelatedsource.jda.utils.WebUtils;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DiscordMemeCommand extends Command {

    public DiscordMemeCommand() {
        this.commandName = "DiscordMeme";
        this.description = "Shows you a discord meme";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"dmeme"};
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getArgs().split("\\s+").length > 0 && event.getArgs().split("\\s+")[0].equalsIgnoreCase("everyone"))
                webUtils.getImageByTag("everyone", image -> MessageHelper.sendFunText(null, image.getUrl() , event));
            else webUtils.getImage("discord_memes", image -> MessageHelper.sendFunText(null, image.getUrl() , event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
