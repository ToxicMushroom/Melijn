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
        this.usage = PREFIX + commandName + " [info] [@user]";
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
                        target = event.getJDA().getUserById(args[1]);
                    else if (event.getMessage().getMentionedUsers().size() > 0)
                        target = event.getMessage().getMentionedUsers().get(0);
                }
                String username = target.getName() + "#" + target.getDiscriminator();
                JSONObject voteObject = PixelSniper.mySQL.getVotesObject(target.getId());
                if (!voteObject.has("votes")) {
                    event.reply(target.getName() + " has never voted >:(\n Go back to start and wait 1 turn or vote (1 turn = 1 year)");
                    return;
                }
                if (event.getGuild() != null) {
                    if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Votes of " + username);
                        eb.setThumbnail(event.getAuthor().getAvatarUrl());
                        eb.setColor(Helpers.EmbedColor);
                        eb.addField("Votes", voteObject.getString("votes"), false);
                        eb.addField("Streak", voteObject.getString("streak"), false);
                        long untilNext = 86400000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                        String untilNextFormat = (untilNext > 0) ? MessageHelper.millisToVote(untilNext) : "none (you can vote now)";
                        eb.addField("Time until next vote", untilNextFormat, false);
                        long untilLoss = 172800000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                        String untilLossFormat = (untilLoss > 0) ? MessageHelper.millisToVote(untilLoss) : "You don't have a streak atm :/";
                        eb.addField("Time until los of streak", untilLossFormat, false);
                        event.reply(eb.build());
                    } else {
                        event.reply("I don't have permissions to send embeds here.. :( (You can send the command in dm)");
                    }
                } else {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Votes of " + username);
                    eb.setThumbnail(target.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.addField("Votes", voteObject.getString("votes"), false);
                    eb.addField("Streak", voteObject.getString("streak"), false);
                    long untilNext = 86_400_000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                    String untilNextFormat = (untilNext > 0) ? MessageHelper.millisToDate(untilNext) : "none (you can vote now)";
                    eb.addField("Time until next vote", untilNextFormat, false);
                    long untilLoss = (86_400_000 * 2) - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                    String untilLossFormat = (untilLoss > 0) ? MessageHelper.millisToDate(untilNext) : "You don't have a streak atm :/";
                    eb.addField("Time until los of streak", untilLossFormat, false);
                }
            }).start();
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
