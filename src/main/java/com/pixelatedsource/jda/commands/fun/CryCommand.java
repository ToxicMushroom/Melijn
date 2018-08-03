package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class CryCommand extends Command {

    public CryCommand() {
        this.commandName = "cry";
        this.description = "cry or let someone make you cry";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        this.aliases = new String[] {"sad"};
        webUtils = WebUtils.getWebUtilsInstance();
    }

    WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                webUtils.getImage("cry", image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** is crying", image.getUrl(), event));
            } else if (args.length == 1) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                if (target == null) {
                    event.reply(event.getAuthor().getAsMention() + " is crying because of rain");
                } else {
                    webUtils.getImage("cry", image ->
                            MessageHelper.sendFunText("**" + target.getName() + "** made **" + event.getAuthor().getName() + "** cry", image.getUrl(), event)
                    );
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
