package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class UnmuteCommand extends Command {

    public UnmuteCommand() {
        this.commandName = "unmute";
        this.description = "Unmutes a muted user";
        this.usage = PREFIX + commandName + " <user> [reason]";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_HISTORY
        };
        this.needs = new Need[]{Need.GUILD};
        this.id = 51;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (event.getArgs().isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            event.getHelpers().retrieveUserByArgsN(event, args[0], user -> {
                if (user == null) {
                    event.reply("Unknown user");
                    return;
                }
                event.async(() -> {
                    String reason = event.getArgs().replaceFirst(args[0], "");
                    if (reason.length() == 0 || reason.matches("\\s+")) reason = "N/A";
                    if (reason.startsWith(" ")) reason = reason.replaceFirst("\\s+", "");
                    if (event.getGuild().isMember(user)) {
                        if (event.getMySQL().unmute(event.getGuild().getMember(user), event.getAuthor(), reason)) {
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    } else {
                        if (event.getMySQL().hardUnmute(event.getGuild().getIdLong(), user.getIdLong(), reason)) {
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    }
                });
            });
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
