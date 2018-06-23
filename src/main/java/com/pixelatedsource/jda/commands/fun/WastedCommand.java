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

public class WastedCommand extends Command {

    public WastedCommand() {
        this.commandName = "wasted";
        this.description = "Be wasted or make wasted";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                    event.reply(new EmbedBuilder()
                            .setColor(Helpers.EmbedColor)
                            .setDescription("**" + event.getAuthor().getName() + "** got WASTED")
                            .setImage(webUtils.getUrl("wasted"))
                            .setFooter("Powered by weeb.sh", null)
                            .build());
                else
                    event.reply("\n" + webUtils.getUrl("wasted"));
            } else if (args.length == 1) {
                User wasted = Helpers.getUserByArgsN(event, args[0]);
                if (wasted == null) {
                    event.reply("Wind got wasted.. wait whatt!");
                } else {
                    if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                        event.reply(new EmbedBuilder()
                                .setColor(Helpers.EmbedColor)
                                .setDescription("**" + wasted.getName() + "** got WASTED by **" + event.getAuthor().getName() + "**")
                                .setImage(webUtils.getUrl("wasted"))
                                .setFooter("Powered by weeb.sh", null)
                                .build());
                    else
                        event.reply("**" + wasted.getName() + "** got WASTEd by **" + event.getAuthor().getName() + "**\n" + webUtils.getUrl("wasted"));
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
