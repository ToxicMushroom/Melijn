package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.ChannelType.SELF_ROLE;

public class SetSelfRoleChannelCommand extends Command {



    public SetSelfRoleChannelCommand() {
        this.commandName = "setSelfRoleChannel";
        this.usage = PREFIX + commandName + " [TextChannel]";
        this.description = "Sets the selfRoleChannel where members can select roles they want";
        this.aliases = new String[]{"ssrc"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.id = 99;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                String s = event.getVariables().selfRolesChannels.getUnchecked(event.getGuild().getIdLong()) == -1 ?
                        "nothing" :
                        "<#" + event.getVariables().selfRolesChannels.getUnchecked(event.getGuild().getIdLong()) + ">";
                event.reply("Current SelfRoleChannel: " + s);
            } else {
                long channel = event.getHelpers().getTextChannelByArgsN(event, args[0]);
                if (channel != -1) {
                    event.getMySQL().setChannel(event.getGuild().getIdLong(), channel, SELF_ROLE);
                    event.reply("The SelfRoleChannel has been changed to <#" + channel + "> by **" + event.getFullAuthorName() + "**");
                } else {
                    event.sendUsage(this, event);
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
