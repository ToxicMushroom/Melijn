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

public class UnbanCommand extends Command {

    public UnbanCommand() {
        this.commandName = "unban";
        this.description = "unban a banned user";
        this.usage = PREFIX + commandName + " <@user | userId>";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[] {
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                if (event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length == 1) {
                        User target = Helpers.getUserByArgsN(event, args[0]);
                        if (target != null) {
                            new Thread(() -> {
                                if (PixelSniper.mySQL.unban(target, event.getGuild(), event.getJDA(), false)) {
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
                    event.reply("I have no permission to unban users.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
