package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Change the looping state or view the looping state";
        this.usage = PREFIX + this.commandName + " [false|true]";
        this.aliases = new String[]{"repeat"};
        this.category = Category.MUSIC;
    }

    public static HashMap<Guild, Boolean> looped = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    String ts = looped.get(event.getGuild()) == null || !looped.get(event.getGuild()) ? "**disabled**." : "**enabled**.";
                    event.reply("Looping is " + ts);
                } else {
                    switch (args[0]) {
                        case "yes":
                        case "true":
                            looped.put(event.getGuild(), true);
                            event.reply("Loop enabled!");
                            break;

                        case "no":
                        case "false":
                            looped.put(event.getGuild(), false);
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
