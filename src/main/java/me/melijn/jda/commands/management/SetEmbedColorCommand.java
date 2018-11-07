package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.ImageUtils;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetEmbedColorCommand extends Command {

    public static final LoadingCache<Long, Integer> embedColorCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Integer load(@NotNull Long key) {
                    return Melijn.mySQL.getEmbedColor(key);
                }
            });

    public SetEmbedColorCommand() {
        this.commandName = "setEmbedColor";
        this.description = "sets an embed color for the bot";
        this.usage = PREFIX + commandName + " [color]";
        this.aliases = new String[]{"sec"};
        this.extra = "The color is formatted as r g b or a hex code";
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 0) {
                event.reply("EmbedColor: " + new Color(embedColorCache.getUnchecked(guild.getIdLong()), false));
            } else if (args.length == 1 && args[0].matches("#?([0-9a-fA-F]{3}){1,2}")) {
                int color = hexToIntColor(args[0].replaceFirst("#", ""));

                Melijn.mySQL.setEmbedColor(guild.getIdLong(), color);
                embedColorCache.put(guild.getIdLong(), color);

                event.reply("The new color is: ", ImageUtils.createPlane(32, color));
            } else if (args.length == 3 && args[0].matches("\\d{1,3}") && args[1].matches("\\d{1,3}") && args[2].matches("\\d{1,3}")) {
                int color = rgbToIntColor(Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]));

                Melijn.mySQL.setEmbedColor(guild.getIdLong(), color);
                embedColorCache.put(guild.getIdLong(), color);

                event.reply("The new color is: ", ImageUtils.createPlane(32, color));
            } else if (args[0].equalsIgnoreCase("null")) {
                Melijn.mySQL.removeEmbedColor(guild.getIdLong());
                embedColorCache.invalidate(guild.getIdLong());

                event.reply("The custom color has been removed");
            } else {
                MessageHelper.sendUsage(this, event);
            }
        }
    }

    private static int hexToIntColor(String hex){
        int red = Integer.valueOf(hex.substring(0, 2), 16);
        int green = Integer.valueOf(hex.substring(2, 4), 16);
        int blue = Integer.valueOf(hex.substring(4, 6), 16);
        red = (red << 16) & 0xFF0000;
        green = (green << 8) & 0x00FF00;
        blue = blue & 0x0000FF;
        return red | green | blue;
    }

    private static int rgbToIntColor(int r, int g, int b){
        r = (r << 16) & 0xFF0000;
        g = (g << 8) & 0x00FF00;
        b = b & 0x0000FF;
        return r | g | b;
    }
}
