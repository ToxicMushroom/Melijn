package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;


public class BanCommand extends Command {

    public BanCommand() {
        this.commandName = "ban";
        this.description = "Bans specified users from your server and gives them a nice message in dm";
        this.usage = Melijn.PREFIX + commandName + " <user> [reason]";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"permban"};
        this.needs = new Need[]{Need.ROLE, Need.GUILD};
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS,
                Permission.MESSAGE_HISTORY
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0  && !args[0].isBlank()) {
                Helpers.retrieveUserByArgsN(event, args[0], target -> {
                    if (target != null) {
                        if (Helpers.canNotInteract(event, target)) return;
                        String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
                        if (reason.length() <= 1000 && Melijn.mySQL.setPermBan(event.getAuthor(), target, event.getGuild(), reason)) {
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    } else {
                        event.reply("Unknown user");
                    }
                });
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
