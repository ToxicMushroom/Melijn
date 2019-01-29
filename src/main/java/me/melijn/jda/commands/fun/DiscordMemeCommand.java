package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class DiscordMemeCommand extends Command {

    public DiscordMemeCommand() {
        this.commandName = "DiscordMeme";
        this.description = "Shows a discord meme";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"dmeme"};
        this.category = Category.FUN;
        this.id = 30;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            if (event.getArgs().split("\\s+").length > 0 && event.getArgs().split("\\s+")[0].equalsIgnoreCase("everyone"))
                event.getWebUtils().getImageByTag("everyone", image -> event.getMessageHelper().sendFunText(null, image.getUrl() , event));
            else event.getWebUtils().getImage("discord_memes", image -> event.getMessageHelper().sendFunText(null, image.getUrl() , event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
