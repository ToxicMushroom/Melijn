package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DabCommand extends Command {

    public DabCommand() {
        this.commandName = "dab";
        this.description = "dab on them haters";
        this.usage = PREFIX + commandName;
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
                        .setDescription("**" + event.getAuthor().getName() + "** is dabbing")
                        .setImage(webUtils.getUrl("dab"))
                        .setFooter("Powered by weeb.sh", null)
                        .build());
            else
                event.reply("**" + event.getAuthor().getName() + "** is dabbing\n" + webUtils.getWeebV1Url("dab") + "\nPowered by weeb.sh");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
