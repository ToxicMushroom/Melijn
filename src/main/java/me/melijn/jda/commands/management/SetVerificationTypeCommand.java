package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetVerificationTypeCommand extends Command {

    public static final LoadingCache<Long, VerificationType> verificationTypes = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public VerificationType load(@NotNull Long key) {
                    return Melijn.mySQL.getVerificationType(key);
                }
            });

    public SetVerificationTypeCommand() {
        this.commandName = "setVerificationType";
        this.usage = PREFIX + commandName + " [code | reCaptcha]";
        this.description = "Sets the verification type";
        this.extra = "reCaptcha uses the melijn site";
        this.aliases = new String[]{"svtype"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 109;
    }

    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                event.reply("The verification mode is set to **" + verificationTypes.getUnchecked(event.getGuildId()).name().toLowerCase() + "**");
                return;
            }
            if (args[0].equalsIgnoreCase("code")) {
                verificationTypes.put(event.getGuildId(), VerificationType.CODE);
                Melijn.mySQL.setVerificationType(event.getGuildId(), VerificationType.CODE);
                event.reply("The verification type has been set to **" + VerificationType.CODE.toString().toLowerCase() + "**");
            } else if (args[0].equalsIgnoreCase("reCaptcha")) {
                verificationTypes.put(event.getGuildId(), VerificationType.RECAPTCHA);
                Melijn.mySQL.setVerificationType(event.getGuildId(), VerificationType.RECAPTCHA);
                event.reply("The verification type has been set to **" + VerificationType.RECAPTCHA.toString().toLowerCase() + "**");
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
