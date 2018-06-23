package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HighfiveCommand extends Command {

    public HighfiveCommand() {
        this.commandName = "highfive";
        this.description = "highfive someone";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                    event.reply(new EmbedBuilder()
                            .setColor(Helpers.EmbedColor)
                            .setDescription("**Melijn** highfived you")
                            .setImage(webUtils.getUrl("highfive"))
                            .setFooter("Powered by weeb.sh", null)
                            .build());
                else
                    event.reply("**Melijn** highfived you\n" + webUtils.getUrl("highfive"));
            } else if (args.length == 1) {
                User author = event.getAuthor();
                User slapped = Helpers.getUserByArgsN(event, args[0]);
                if (slapped == null) {
                    event.reply(event.getAuthor().getAsMention() + " is highfiving the air lol");
                } else {
                    if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                        event.reply(new EmbedBuilder()
                                .setColor(Helpers.EmbedColor)
                                .setDescription("**" + author.getName() + "** highfived **" + slapped.getName() + "**")
                                .setImage(webUtils.getUrl("highfive"))
                                .setFooter("Powered by weeb.sh", null)
                                .build());
                    else
                        event.reply("**" + author.getName() + "** highfive **" + slapped.getName() + "**\n" + webUtils.getUrl("highfive"));
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
