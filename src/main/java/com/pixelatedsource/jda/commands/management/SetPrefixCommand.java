package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Arrays;
import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetPrefixCommand extends Command {

    public SetPrefixCommand() {
        this.commandName = "setPrefix";
        this.description = "Change the prefix for the commands for your guild";
        this.usage = PREFIX + this.commandName + " [prefix]";
        this.aliases = new String[]{"prefix"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, String> prefixes = PixelSniper.mySQL.getPrefixMap();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) event.reply(prefixes.containsKey(guild.getIdLong()) ? prefixes.get(guild.getIdLong()) : PREFIX);
                else if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                    if (Arrays.toString(args).length() <= 100) {
                        PixelSniper.MAIN_THREAD.submit(() -> {
                            PixelSniper.mySQL.setPrefix(guild.getIdLong(), args[0]);
                            if (prefixes.replace(guild.getIdLong(), args[0]) == null)
                                prefixes.put(guild.getIdLong(), args[0]);
                            event.reply("The prefix has been set to `" + args[0] + "`");
                        });
                    } else {
                        event.reply("The maximum prefix size is 100 characters");
                    }
                } else {
                    event.reply("You need the permission `" + commandName + "` to change the prefix.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
