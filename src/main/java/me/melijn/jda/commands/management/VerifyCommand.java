package me.melijn.jda.commands.management;

import com.google.common.collect.Sets;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.Map;

import static me.melijn.jda.Melijn.PREFIX;

public class VerifyCommand extends Command {

    public VerifyCommand() {
        this.commandName = "verify";
        this.description = "Verifies a member";
        this.usage = PREFIX + commandName + " <user | all>";
        this.needs = new Need[]{Need.GUILD};
        this.permissions = new Permission[]{Permission.MANAGE_ROLES};
        this.category = Category.MANAGEMENT;
        this.id = 85;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (event.getArgs().isEmpty()) {

                event.sendUsage(this, event);
                return;
            }
            if (args[0].equals("all")) {
                Map<Long, Long> unVerifiedUsers = event.getVariables().unVerifiedGuildMembersCache.get(event.getGuild().getIdLong());
                verifyMembers(event, event.getGuild(), unVerifiedUsers);
                event.reply("Successfully verified all unverified members");
            } else {
                User user = event.getHelpers().getUserByArgsN(event, args[0]);
                if (user != null && event.getGuild().getMember(user) != null) {
                    Map<Long, Long> unVerifiedUsers = event.getVariables().unVerifiedGuildMembersCache.get(event.getGuild().getIdLong());
                    if (unVerifiedUsers.keySet().contains(user.getIdLong())) {
                        event.getHelpers().verify(event.getGuild(), user);
                        event.reply("Successfully verified **" + user.getName() + "#" + user.getDiscriminator() + "**");
                    } else {
                        event.reply("This member is already verified");
                    }
                } else {
                    event.reply("Unknown member");
                }
            }

        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void verifyMembers(CommandEvent event, Guild guild, Map<Long, Long> users) {
        event.async(() -> {
            for (long id : Sets.newHashSet(users.keySet())) {
                Member member = guild.getMemberById(id);
                if (member != null)
                    event.getHelpers().verify(guild, member.getUser());
            }
        });
    }
}
