package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class WastedCommand extends Command {

    private WebUtils webUtils;

    public WastedCommand() {
        this.commandName = "wasted";
        this.description = "Shows a wasted gif [anime]";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
        this.id = 23;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isBlank()) {
                webUtils.getImage("wasted",
                        image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** got WASTED", image.getUrl(), event)
                );
            } else if (args.length == 1) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                if (target == null) {
                    event.reply("Wind got wasted.. wait whatt!");
                } else {
                    webUtils.getImage("wasted",
                            image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** got wasted by **" + target.getName() + "**", image.getUrl(), event)
                    );
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
