package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class KickCommand extends Command {

    public KickCommand() {
        this.commandName = "kick";
        this.description = "kick a member";
        this.usage = PREFIX + commandName + " <member> [reason]";
        this.category = Category.MANAGEMENT;
        this.extra = "the bot will dm the reason to the target if one is provided";
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
        this.permissions = new Permission[]{Permission.KICK_MEMBERS,
                Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
                if (target != null) {
                    if (event.getGuild().getMember(target) != null) {
                        new Thread(() -> {
                            if (event.getGuild().getMember(target).getRoles().size() > 0 && event.getGuild().getSelfMember().getRoles().size() > 0) {
                                if (event.getGuild().getSelfMember().getRoles().get(0).canInteract(event.getGuild().getMember(target).getRoles().get(0))) {
                                    event.reply("Can't modify a member with higher or equal highest role than myself");
                                    return;
                                }
                            }
                            if (reason.length() <= 1000 && PixelSniper.mySQL.addKick(event.getAuthor(), target, event.getGuild(), reason)) {
                                event.getMessage().addReaction("\u2705").queue();
                            } else {
                                event.getMessage().addReaction("\u274C").queue();
                            }
                        }).start();
                    } else {
                        event.reply("This user isn't a member of this guild");
                    }
                } else {
                    event.reply("Unknown user");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
