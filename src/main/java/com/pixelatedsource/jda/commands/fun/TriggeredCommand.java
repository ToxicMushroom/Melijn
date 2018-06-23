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

public class TriggeredCommand extends Command {

    public TriggeredCommand() {
        this.commandName = "triggered";
        this.description = "Will visualize your triggered state to other people";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"rage"};
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
                            .setDescription("**" + event.getAuthor().getName() + "** is triggered")
                            .setImage(webUtils.getUrl("triggered"))
                            .setFooter("Powered by weeb.sh", null)
                            .build());
                else
                    event.reply("**" + event.getAuthor().getName() + "** is triggered" + "\n" + webUtils.getUrl("triggered"));
            } else if (args.length == 1) {
                User triggered = Helpers.getUserByArgsN(event, args[0]);
                if (triggered == null) {
                    event.reply("The wind is trigge.. NO, stop it");
                } else {
                    if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                        event.reply(new EmbedBuilder()
                                .setColor(Helpers.EmbedColor)
                                .setDescription("**" + triggered.getName() + "** triggered **" + event.getAuthor().getName() + "**")
                                .setImage(webUtils.getUrl("triggered"))
                                .setFooter("Powered by weeb.sh", null)
                                .build());
                    else
                        event.reply("**" + triggered.getName() + "** triggered **" + event.getAuthor().getName() + "**" + "\n" + webUtils.getUrl("triggered"));
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
