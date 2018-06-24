package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ShrugCommand extends Command {

    public ShrugCommand() {
        this.commandName = "shrug";
        this.description = "shrug";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"idc"};
        webUtils = WebUtils.getWebUtilsInstance();
    }

    WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("**" + event.getAuthor().getName() + "** shrugs")
                        .setImage(webUtils.getUrl("shrug"))
                        .setFooter("Powered by weeb.sh", null)
                        .build());
            else
                event.reply("**" + event.getAuthor().getName() + "** shrugs\n" + webUtils.getWeebV1Url("shrug") + "\nPowered by weeb.sh");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
