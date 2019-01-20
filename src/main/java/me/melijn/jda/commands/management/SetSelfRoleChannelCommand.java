package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.ChannelType.SELF_ROLE;

public class SetSelfRoleChannelCommand extends Command {

    public static final LoadingCache<Long, Long> selfRolesChannel = CacheBuilder.newBuilder()
            .maximumSize(30)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getChannelId(key, SELF_ROLE);
                }
            });

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
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                String s = selfRolesChannel.getUnchecked(event.getGuild().getIdLong()) == -1 ? "nothing" : "<#" + selfRolesChannel.getUnchecked(event.getGuild().getIdLong()) + ">";
                event.reply("Current SelfRoleChannel: " + s);
            } else {
                long channel = Helpers.getTextChannelByArgsN(event, args[0]);
                if (channel != -1) {
                    Melijn.mySQL.setChannel(event.getGuild().getIdLong(), channel, SELF_ROLE);
                    event.reply("The SelfRoleChannel has been changed to <#" + channel + "> by **" + event.getFullAuthorName() + "**");
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
