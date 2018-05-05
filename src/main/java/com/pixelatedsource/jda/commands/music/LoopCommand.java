package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Change the looping state or view the looping state";
        this.usage = PREFIX + this.commandName + " [false|true]";
        this.aliases = new String[]{"repeat"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    String ts = PixelSniper.looped.get(event.getGuild()) == null || !PixelSniper.looped.get(event.getGuild()) ? "**disabled**." : "**enabled**.";
                    event.reply("Looping is " + ts);
                } else {
                    switch (args[0]) {
                        case "yes":
                        case "true":
                            PixelSniper.looped.put(event.getGuild(), true);
                            event.reply("Loop enabled!");
                            break;

                        case "no"://idk if switch works like this
                        case "false":
                            PixelSniper.looped.put(event.getGuild(), false);
                            event.reply("Loop disabled!");
                            break;
                        default:
                            MessageHelper.sendUsage(this, event);
                            break;
                    }
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
