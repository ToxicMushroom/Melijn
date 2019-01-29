package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;

public class PollCommand extends Command {

    public PollCommand() {
        this.commandName = "poll";
        this.description = "Creates a poll";
        this.usage = PREFIX + commandName + " [channel] <\"question\"> <\"answer1\"> <\"answer2\"> [\"up to 9 answers...\"]";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"createPoll"};
        this.extra = "example: >poll #announcements 7d \"Which is better?\" \"Minecraft\" \"Roblox\"";
        this.id = 102;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            if (event.getArgs().isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            String[] args = event.getArgs().substring(0, event.getArgs().replaceFirst("\".*", "").length()).split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                if (noEnoughArguments(event)) return;
                String question = event.getArgs().replaceFirst("\"", "").substring(0, event.getArgs().replaceFirst("\"", "").indexOf("\""));
                String answersString = event.getArgs().replaceFirst(Pattern.quote("\"" + question + "\"") + "(?:\\s+)?", "");
                String[] answers = answersString.split("\"(?:\\s+)?\"");
                answers[0] = answers[0].replaceFirst("\"", "");
                answers[answers.length-1] = answers[answers.length-1].substring(0, answers[answers.length-1].length()-1);
                StringBuilder sb = new StringBuilder(question);
                int count = 0;
                for (String answer : answers) {
                    sb.append("\n").append(++count).append(". ").append(answer);
                }
                event.getTextChannel().sendMessage(sb.toString()).queue(message -> addReactions(message, answers));
            } else if (args.length == 1) {
                TextChannel textChannel = event.getGuild().getTextChannelById(event.getHelpers().getTextChannelByArgsN(event, args[0]));
                if (textChannel == null) {
                    event.sendUsage(this, event);
                    return;
                }
                if (!event.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_WRITE)) {
                    event.reply("I need the permission **Message Write** to post a poll in that channel.");
                    return;
                }
                if (noEnoughArguments(event)) return;
                String question = event.getArgs().replaceFirst(Pattern.quote(args[0]) + "(?:\\s+)?\"", "")
                        .substring(0, event.getArgs().replaceFirst(Pattern.quote(args[0]) + "(?:\\s+)?\"", "").indexOf("\""));
                String answersString = event.getArgs().replaceFirst(Pattern.quote(args[0]) + "(?:\\s+)?" + Pattern.quote("\"" + question + "\"") + "(?:\\s+)?", "");
                String[] answers = answersString.split("\"(?:\\s+)?\"");
                answers[0] = answers[0].replaceFirst("\"", "");
                answers[answers.length-1] = answers[answers.length-1].substring(0, answers[answers.length-1].length()-1);
                StringBuilder sb = new StringBuilder(question);
                int count = 0;
                for (String answer : answers) {
                    sb.append("\n").append(++count).append(". ").append(answer);
                }
                textChannel.sendMessage(sb.toString()).queue(message -> addReactions(message, answers));
            } else {
                event.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private boolean noEnoughArguments(CommandEvent event) {
        int amount = StringUtils.countMatches(event.getArgs(), "\"");
        if (amount < 6 || amount > 20 || (amount & 1) == 1) {
            event.sendUsage(this, event);
            return true;
        }
        return false;
    }

    private void addReactions(Message message, String[] answers) {
        if (answers.length >= 2) {
            message.addReaction("\u0031\u20E3").queue();
            message.addReaction("\u0032\u20E3").queue();
        }
        if (answers.length >= 3)
            message.addReaction("\u0033\u20E3").queue();
        if (answers.length >= 4)
            message.addReaction("\u0034\u20E3").queue();
        if (answers.length >= 5)
            message.addReaction("\u0035\u20E3").queue();
        if (answers.length >= 6)
            message.addReaction("\u0036\u20E3").queue();
        if (answers.length >= 7)
            message.addReaction("\u0037\u20E3").queue();
        if (answers.length >= 8)
            message.addReaction("\u0038\u20E3").queue();
        if (answers.length >= 9)
            message.addReaction("\u0039\u20E3").queue();
    }
}
