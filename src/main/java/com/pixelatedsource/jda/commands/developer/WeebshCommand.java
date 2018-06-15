package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.pixelatedsource.jda.utils.WebUtils;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class WeebshCommand extends Command {

    public WeebshCommand() {
        this.commandName = "weebsh";
        this.description = "Uses weebsh api to do stuff";
        this.usage = PREFIX + commandName + " <getTags|getTypes|type|tag> [searchTerm]";
        this.category = Category.DEVELOPER;
    }

    WebUtils webUtils = WebUtils.getWebUtilsInstance();

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "gettags":
                    event.reply(webUtils.getTags().toString());
                    break;
                case "gettypes":
                    event.reply(webUtils.getTypes().toString());
                    break;
                case "type":
                    if (args.length > 1) {
                        event.reply(webUtils.getUrl(event.getArgs().replaceFirst(args[0] + "\\s+", "")));
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                    break;
                case "tag":
                    if (args.length > 1) {
                        event.reply(webUtils.getUrlByTag(event.getArgs().replaceFirst(args[0] + "\\s+", "")));
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                    break;
                default:
                    MessageHelper.sendUsage(this, event);
                    break;
            }
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
