package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class WarnCommand extends Command {

    public WarnCommand() {
        this.commandName = "warn";
        this.description = "warn someone";
        this.usage = PREFIX + commandName + " <user> <reason>";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 1) {
                    User target = Helpers.getUserByArgsN(event, args[0]);
                    if (target != null && event.getGuild().getMember(target) != null) {
                        String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
                        new Thread(() -> {
                            if (event.getGuild().getMember(target) != null) {
                                if (reason.length() <= 1000 && PixelSniper.mySQL.addWarn(event.getAuthor(), target, event.getGuild(), reason)) {
                                    event.getMessage().addReaction("\u2705").queue();
                                } else {
                                    event.getMessage().addReaction("\u274C").queue();
                                }
                            } else {
                                event.reply("This user isn't a member of this guild");
                            }
                        }).start();
                    } else {
                        event.reply("Unknown member");
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
