package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetJoinRoleCommand extends Command {

    public static final LoadingCache<Long, Long> joinRoleCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getRoleId(key, RoleType.JOIN);
                }
            });

    public SetJoinRoleCommand() {
        this.commandName = "setJoinRole";
        this.description = "Setup a role that a user get's on join";
        this.usage = PREFIX + commandName + " [role | null]";
        this.aliases = new String[]{"sjr"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = joinRoleCache.getUnchecked(guild.getIdLong());
            if (args.length == 0 || args[0].isBlank()) {
                if (role != -1 && guild.getRoleById(role) != null)
                    event.reply("Current JoinRole: **@" + guild.getRoleById(role).getName() + "**");
                else event.reply("Current JoinRole is unset");
            } else {
                if (args[0].equalsIgnoreCase("null")) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeRole(guild.getIdLong(), RoleType.JOIN);
                        joinRoleCache.invalidate(guild.getIdLong());
                    });
                    event.reply("JoinRole has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    Role joinRole = Helpers.getRoleByArgs(event, args[0]);
                    if (joinRole != null) {
                        if (joinRole.getIdLong() != guild.getIdLong()) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).canInteract(joinRole)) {
                                TaskScheduler.async(() -> {
                                    Melijn.mySQL.setRole(guild.getIdLong(), joinRole.getIdLong(), RoleType.JOIN);
                                    joinRoleCache.put(guild.getIdLong(), joinRole.getIdLong());
                                });
                                event.reply("JoinRole changed to **@" + joinRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            } else {
                                event.reply("The JoinRole hasn't been changed due: **@" + joinRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                            }
                        } else {
                            event.reply("The @everyone role cannot be as the JoinRole because everyone has it");
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
