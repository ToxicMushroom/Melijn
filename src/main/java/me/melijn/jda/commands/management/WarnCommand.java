package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class WarnCommand extends Command {

    public WarnCommand() {
        this.commandName = "warn";
        this.description = "Warns a member";
        this.usage = PREFIX + commandName + " <member> <reason>";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{Permission.MESSAGE_HISTORY};
        this.needs = new Need[]{Need.GUILD};
        this.id = 54;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (event.getArgs().isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            User target = event.getHelpers().getUserByArgsN(event, args[0]);
            if (target == null || event.getGuild().getMember(target) == null) {
                event.reply("Unknown member");
                return;
            }
            String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
            event.async(() -> {
                if (event.getGuild().getMember(target) != null) {
                    if (reason.length() <= 1000 && event.getMySQL().addWarn(event.getAuthor(), target, event.getGuild(), reason)) {
                        event.getMessage().addReaction("\u2705").queue();
                    } else {
                        event.getMessage().addReaction("\u274C").queue();
                    }
                } else {
                    event.reply("This user isn't a member of this guild");
                }
            });
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
