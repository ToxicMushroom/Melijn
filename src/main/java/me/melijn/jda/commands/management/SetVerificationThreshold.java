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
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationThreshold extends Command {

    public SetVerificationThreshold() {
        this.commandName = "setVerificationThreshold";
        this.usage = PREFIX + commandName + " <0 - 20>";
        this.description = "Set the verification threshold before kicking";
        this.aliases = new String[]{"svt"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.extra = "0 disables the threshold any higher number is the amount of times the user can answer incorrect before getting kicked";
    }

    public static final LoadingCache<Long, Integer> verificationThresholdCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Integer load(@NotNull Long key) {
                    return Melijn.mySQL.getGuildVerificationThreshold(key);
                }
            });

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length > 0 && args[0].matches("^([0-1]?[0-9]|20)$")) {
                int i = Integer.parseInt(args[0]);
                if (i == 0) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeVerificationThreshold(guild.getIdLong());
                        verificationThresholdCache.invalidate(guild.getIdLong());
                    });
                    event.reply("The VerificationThreshold has been disabled by **" + event.getFullAuthorName() + "**");
                } else {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.setVerificationThreshold(guild.getIdLong(), i);
                        verificationThresholdCache.put(guild.getIdLong(), i);
                    });
                    event.reply("The VerificationThreshold has been set to **" + i + "** by **" + event.getFullAuthorName() + "**");
                }
            } else {
                event.reply("The VerificationThreshold is **" + (verificationThresholdCache.getUnchecked(guild.getIdLong()) == 0 ? "disabled" : verificationThresholdCache.getUnchecked(guild.getIdLong())) + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
