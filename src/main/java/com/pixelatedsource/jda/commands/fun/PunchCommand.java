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

public class PunchCommand extends Command {

    public PunchCommand() {
        this.commandName = "punch";
        this.description = "You can punch someone or be punched by Melijn";
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
                            .setDescription("**" + event.getAuthor().getName() + "** got punched by **" + event.getJDA().getSelfUser().getName() + "**")
                            .setImage(webUtils.getUrl("punch"))
                            .setFooter("Powered by weeb.sh", null)
                            .build());
                else
                    event.reply("**" + event.getAuthor().getName() + "** got punched by **" + event.getJDA().getSelfUser().getName() + "**" + "\n" + webUtils.getUrl("punch"));
            } else if (args.length == 1) {
                User punched = Helpers.getUserByArgsN(event, args[0]);
                if (punched == null) {
                    event.reply(event.getAuthor().getAsMention() + " is punching at air >.<");
                } else {
                    if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                        event.reply(new EmbedBuilder()
                                .setColor(Helpers.EmbedColor)
                                .setDescription("**" + punched.getName() + "** got punched by **" + event.getAuthor().getName() + "**")
                                .setImage(webUtils.getUrl("punch"))
                                .setFooter("Powered by weeb.sh", null)
                                .build());
                    else
                        event.reply("**" + punched.getName() + "** got punched by **" + event.getAuthor().getName() + "**\n" + webUtils.getUrl("punch"));
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
