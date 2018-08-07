package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONObject;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class VoteCommand extends Command {

    public VoteCommand() {
        this.commandName = "vote";
        this.description = "gives you the vote link to support our bot";
        this.aliases = new String[]{"donate"};
        this.usage = PREFIX + commandName + " [info] [user]";
        this.category = Category.DEFAULT;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].equalsIgnoreCase("")) {
            event.reply("Support us by voting and get access to locked commands\nhttps://discordbots.org/bot/melijn/vote");
        } else if (args[0].equalsIgnoreCase("info")) {
            PixelSniper.MAIN_THREAD.submit(() -> {
                User target = Helpers.getUserByArgs(event, args.length > 1 ? args[1] : "");
                String username = target.getName() + "#" + target.getDiscriminator();
                JSONObject voteObject = PixelSniper.mySQL.getVotesObject(target.getIdLong());
                if (!voteObject.has("votes")) {
                    event.reply(target.getName() + " has never voted.");
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Votes of " + username);
                eb.setThumbnail(target.getAvatarUrl());
                eb.setColor(Helpers.EmbedColor);
                eb.addField("Votes", String.valueOf(voteObject.getLong("votes")), false);
                eb.addField("Streak", String.valueOf(voteObject.getLong("streak")), false);
                long untilNext = 43_200_000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                String untilNextFormat = (untilNext > 0) ? MessageHelper.millisToVote(untilNext) : "none (you can vote now)";
                eb.addField("Time until next vote", untilNextFormat, false);
                long untilLoss = 172800000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                String untilLossFormat = (untilLoss > 0) ? MessageHelper.millisToVote(untilLoss) : "You don't have a streak atm :/";
                eb.addField("Time until los of streak", untilLossFormat, false);
                if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
                    event.reply(eb.build());
                } else {
                    event.reply("I don't have permissions to send embeds here.. :( (You can send the command in dm)");
                }
            });
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
