package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class BanCommand extends Command {

    public BanCommand() {
        this.commandName = "ban";
        this.description = "Bans specified users from your server and gives them a nice message in dm";
        this.usage = PREFIX + commandName + " <user> [reason]";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"permban"};
        this.needs = new Need[]{Need.ROLE, Need.GUILD};
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0  && !args[0].equalsIgnoreCase("")) {
                Helpers.retrieveUserByArgsN(event, args[0], target -> {
                    if (target != null) {
                        if (event.getGuild().getMember(target).getRoles().size() > 1) {
                            if (event.getGuild().getMember(target).getRoles().get(0).getPosition() <= event.getGuild().getSelfMember().getRoles().get(0).getPosition()) {
                                event.reply("I can't modify a member with higher or equal highest role than myself");
                                return;
                            }
                        }
                        String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
                        if (reason.length() <= 1000 && PixelSniper.mySQL.setPermBan(event.getAuthor(), target, event.getGuild(), reason)) {
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
