package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.CrapUtils;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class PunchCommand extends Command {

    private CrapUtils crapUtils;

    public PunchCommand() {
        this.commandName = "punch";
        this.description = "Shows a person punching [anime]";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        crapUtils = CrapUtils.getWebUtilsInstance();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isBlank()) {
                crapUtils.getImage("punch",
                        image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** wants to punch someone", image.getUrl(), event)
                );
            } else if (args.length == 1) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                if (target == null) {
                    event.reply(event.getAuthor().getAsMention() + " is punching air >.<");
                } else {
                    crapUtils.getImage("punch",
                            image -> MessageHelper.sendFunText("**" + target.getName() + "** got punched by **" + event.getAuthor().getName() + "**", image.getUrl(), event)
                    );
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
