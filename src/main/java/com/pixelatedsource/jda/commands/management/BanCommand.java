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

public class BanCommand extends Command {

    public BanCommand() {
        this.commandName = "ban";
        this.description = "Bans users from your server and give them a nice message in pm.";
        this.usage = PREFIX + commandName + " <@user | userId> <reason>";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"permban"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length >= 2) {
                        User target = Helpers.getUserByArgsN(event, args[0]);
                        String reason = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                        if (target != null) {
                            new Thread(() -> {
                                if (PixelSniper.mySQL.setPermBan(event.getAuthor(), target, event.getGuild(), reason)) {
                                    event.getMessage().addReaction("\u2705").queue();
                                } else {
                                    event.getMessage().addReaction("\u274C").queue();
                                }
                            });
                        } else {
                            event.reply("Unknown user");
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                } else {
                    event.reply("I have no permission to ban users.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
