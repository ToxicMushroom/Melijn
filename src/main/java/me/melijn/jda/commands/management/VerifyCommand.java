package me.melijn.jda.commands.management;

import gnu.trove.list.TLongList;
import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.events.JoinLeave;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class VerifyCommand extends Command {

    public VerifyCommand() {
        this.commandName = "verify";
        this.description = "Manually verify a member";
        this.usage = PREFIX + commandName + " <user | all>";
        this.needs = new Need[]{Need.GUILD};
        this.permissions = new Permission[]{Permission.MANAGE_ROLES};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                User user = Helpers.getUserByArgsN(event, args[0]);
                if (user != null && event.getGuild().getMember(user) != null) {
                    TLongList unVerifiedUsers = JoinLeave.unVerifiedGuildMembersCache.getUnchecked(event.getGuild().getIdLong());
                    if (unVerifiedUsers.contains(user.getIdLong())) {
                        JoinLeave.verify(event.getGuild(), user);
                        event.reply("Successfully verified **" + user.getName() + "#" + user.getDiscriminator() + "**");
                    } else {
                        event.reply("This member is already verified");
                    }
                } else {
                    event.reply("Unknown member");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }
}
