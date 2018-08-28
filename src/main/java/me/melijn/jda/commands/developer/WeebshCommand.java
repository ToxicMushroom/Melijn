package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;

import java.util.Arrays;

import static me.melijn.jda.Melijn.PREFIX;

public class WeebshCommand extends Command {

    public WeebshCommand() {
        this.commandName = "weebsh";
        this.description = "Uses weebsh api to do stuff";
        this.usage = PREFIX + commandName + " <tags | types | type | tag> [arg]";
        this.category = Category.DEVELOPER;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "tags":
                    webUtils.getTags(tags -> event.reply(Arrays.toString(tags.toArray())));
                    break;
                case "types":
                    webUtils.getTypes(types -> event.reply(Arrays.toString(types.getTypes().toArray())));
                    break;
                case "type":
                    if (args.length > 1) {
                       webUtils.getImage(event.getArgs().replaceFirst(args[0] + "\\s+", ""), (image) -> event.reply(image.getUrl()));
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                    break;
                case "tag":
                    if (args.length > 1) {
                        webUtils.getImageByTag(event.getArgs().replaceFirst(args[0] + "\\s+", ""), (image) -> event.reply(image.getUrl()));
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
