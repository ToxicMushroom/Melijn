package me.melijn.jda.commands;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONObject;

import static me.melijn.jda.Melijn.PREFIX;

public class VoteCommand extends Command {

    public VoteCommand() {
        this.commandName = "vote";
        this.description = "Gives the vote link to support our bot";
        this.usage = PREFIX + commandName + " [info] [user]";
        this.category = Category.DEFAULT;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.id = 33;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            event.reply("Support us by voting and get access to locked commands\nhttps://discordbots.org/bot/melijn/vote");
        } else if (args[0].equalsIgnoreCase("info")) {
            event.async(() -> {
                User target = event.getHelpers().getUserByArgs(event, args.length > 1 ? args[1] : "");
                String username = target.getName() + "#" + target.getDiscriminator();

                JSONObject voteObject = event.getMySQL().getVotesObject(target.getIdLong());
                if (!voteObject.has("votes")) {
                    event.reply(target.getName() + " has never voted.");
                    return;
                }

                EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                eb.setTitle("Votes of " + username);
                eb.setThumbnail(target.getAvatarUrl());
                eb.addField("Votes", String.valueOf(voteObject.getLong("votes")), false);
                eb.addField("Streak", String.valueOf(voteObject.getLong("streak")), false);

                long untilNext = 43_200_000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                String untilNextFormat = (untilNext > 0) ? event.getMessageHelper().millisToVote(untilNext) : "none (you can vote now)";
                eb.addField("Time until next vote", untilNextFormat, false);

                long untilLoss = 172800000 - (System.currentTimeMillis() - voteObject.getLong("lastTime"));
                String untilLossFormat = (untilLoss > 0) ? event.getMessageHelper().millisToVote(untilLoss) : "You don't have a streak atm :/";
                eb.addField("Time until los of streak", untilLossFormat, false);

                event.reply(eb.build());
            });
        } else {
            event.sendUsage(this, event);
        }
    }
}
