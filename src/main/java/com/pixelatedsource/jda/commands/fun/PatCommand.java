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

public class PatCommand extends Command {

    public PatCommand() {
        this.commandName = "pat";
        this.description = "You can pat someone or be patted";
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
                            .setDescription("**Melijn** patted you")
                            .setImage(webUtils.getUrl("pat"))
                            .setFooter("Powered by weeb.sh", null)
                            .build());
                else
                    event.reply("**Melijn** patted you\n" + webUtils.getUrl("pat"));
            } else if (args.length == 1) {
                User patted = Helpers.getUserByArgsN(event, args[0]);
                if (patted == null) {
                    event.reply("Didn't catch that? Try harder");
                } else {
                    if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                        event.reply(new EmbedBuilder()
                                .setColor(Helpers.EmbedColor)
                                .setDescription("**" + event.getAuthor().getName() + "** patted **" + patted.getName() + "**")
                                .setImage(webUtils.getUrl("pat"))
                                .setFooter("Powered by weeb.sh", null)
                                .build());
                    else
                        event.reply("**Melijn** patted you\n" + webUtils.getUrl("slap"));
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
