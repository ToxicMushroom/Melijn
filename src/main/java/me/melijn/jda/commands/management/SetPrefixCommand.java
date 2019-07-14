package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Arrays;

import static me.melijn.jda.Melijn.PREFIX;

public class SetPrefixCommand extends Command {

    public SetPrefixCommand() {
        this.commandName = "setPrefix";
        this.description = "Sets the prefix for the commands of Melijn";
        this.usage = PREFIX + commandName + " [prefix]";
        this.aliases = new String[]{"prefix"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 78;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 0)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty())
                event.reply(event.getVariables().prefixes.get(event.getGuild().getIdLong()));
            else if (event.hasPerm(event.getMember(), commandName, 1)) {
                if (Arrays.toString(args).length() <= 10) {
                    event.async(() -> {
                        event.getMySQL().setPrefix(guild.getIdLong(), args[0]);
                        event.reply("The prefix has been set to `" + args[0] + "`");
                    });
                } else {
                    event.reply("The maximum prefix size is 10 characters");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to change the prefix.");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
