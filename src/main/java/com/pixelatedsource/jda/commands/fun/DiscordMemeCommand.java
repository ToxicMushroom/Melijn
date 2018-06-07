package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DiscordMemeCommand extends Command {

    public DiscordMemeCommand() {
        this.commandName = "DiscordMeme";
        this.description = "Shows you a nice meme";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"dmeme"};
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("**Discord Meme**")
                        .setImage(webUtils.getUrl("discord_memes"))
                        .build());
            else
                event.reply("**Discord Meme** \n" + webUtils.getUrl("discord_memes"));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
