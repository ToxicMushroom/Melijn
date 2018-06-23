package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LewdCommand extends Command {

    public LewdCommand() {
        this.commandName = "lewd";
        this.description = "Shows a lewd image";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("**" + event.getAuthor().getName() + "** is being lewd")
                        .setImage(webUtils.getUrl("lewd"))
                        .setFooter("Powered by weeb.sh", null)
                        .build());
            else
                event.reply("**" + event.getAuthor().getName() + "** is being lewd\n" + webUtils.getWeebV1Url("potato") + "\nPowered by weeb.sh");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
