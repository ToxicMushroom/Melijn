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

public class SetUnverifiedRoleCommand extends Command {

    public static final LoadingCache<Long, Long> unverifiedRoleCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getRoleId(key, RoleType.UNVERIFIED);
                }
            });

    public SetUnverifiedRoleCommand() {
        this.commandName = "setUnverifiedRole";
        this.usage = PREFIX + commandName + " [role | null]";
        this.description = "Sets the UnverifiedRole that will be added to unverified members when they join";
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"sur"};
        this.id = 6;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = unverifiedRoleCache.getUnchecked(guild.getIdLong());
            if (args.length == 0 || args[0].isBlank()) {
                if (role != -1 && guild.getRoleById(role) != null)
                    event.reply("Current UnverifiedRole: **@" + guild.getRoleById(role).getName() + "**");
                else event.reply("Current UnverifiedRole is unset");
            } else {
                if (args[0].equalsIgnoreCase("null")) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeRole(guild.getIdLong(), RoleType.UNVERIFIED);
                        unverifiedRoleCache.invalidate(guild.getIdLong());
                    });
                    event.reply("UnverifiedRole has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    Role unverifiedRole = Helpers.getRoleByArgs(event, args[0]);
                    if (unverifiedRole != null) {
                        if (unverifiedRole.getIdLong() != guild.getIdLong()) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).canInteract(unverifiedRole)) {
                                TaskScheduler.async(() -> {
                                    Melijn.mySQL.setRole(guild.getIdLong(), unverifiedRole.getIdLong(), RoleType.UNVERIFIED);
                                    unverifiedRoleCache.put(guild.getIdLong(), unverifiedRole.getIdLong());
                                });
                                event.reply("UnverifiedRole changed to **@" + unverifiedRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            }
                            else {
                                event.reply("The UnverifiedRole hasn't been changed due: **@" + unverifiedRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                            }
                        } else {
                            event.reply("The @everyone role cannot be as the UnverifiedRole because everyone has it");
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
