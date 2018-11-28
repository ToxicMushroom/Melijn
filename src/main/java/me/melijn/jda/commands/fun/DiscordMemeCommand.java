package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.CrapUtils;

import static me.melijn.jda.Melijn.PREFIX;

public class DiscordMemeCommand extends Command {

    private CrapUtils crapUtils;

    public DiscordMemeCommand() {
        this.commandName = "DiscordMeme";
        this.description = "Shows you a discord meme";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"dmeme"};
        this.category = Category.FUN;
        crapUtils = CrapUtils.getWebUtilsInstance();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getArgs().split("\\s+").length > 0 && event.getArgs().split("\\s+")[0].equalsIgnoreCase("everyone"))
                crapUtils.getImageByTag("everyone", image -> MessageHelper.sendFunText(null, image.getUrl() , event));
            else crapUtils.getImage("discord_memes", image -> MessageHelper.sendFunText(null, image.getUrl() , event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
