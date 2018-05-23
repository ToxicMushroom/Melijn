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
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].equalsIgnoreCase("")) {
            event.reply("Support us by voting and get rewarded: https://discordbots.org/bot/368362411591204865/vote");
        } else if (args[0].equalsIgnoreCase("info")) {
            new Thread(() -> {
                User target = event.getAuthor();
                if (args.length > 1) {
                    if (args[1].matches("\\d+") && event.getJDA().getUserById(args[1]) != null)
                        target = event.getJDA().retrieveUserById(args[1]).complete();
                    else if (event.getMessage().getMentionedUsers().size() > 0)
                        target = event.getMessage().getMentionedUsers().get(0);
                    else if (event.getGuild().getMembersByName(args[1], true).size() > 0)
                        target = event.getGuild().getMembersByName(args[1], true).get(0).getUser();
                    else if (event.getJDA().getUsersByName(args[0], true).size() > 0)
                        target = event.getJDA().getUsersByName(args[0], true).get(0);
                }
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
                long untilNext = 86400000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
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
            }).start();
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
