package me.melijn.jda.events;

import com.google.common.cache.CacheLoader;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogOption;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.Ban;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.json.JSONObject;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;

public class Chat extends ListenerAdapter {

    private List<User> black = new ArrayList<>();
    private MySQL mySQL = Melijn.mySQL;
    private String latestId = "";
    private int latestChanges = 0;
    private HashMap<Long, HashMap<Long, Integer>> guildUserVerifyTries = new HashMap<>();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            if (event.getMessage().getContentRaw().equalsIgnoreCase(PREFIX) || event.getMessage().getContentRaw().equalsIgnoreCase(event.getJDA().getSelfUser().getAsMention()))
                event.getChannel().sendMessage(String.format("Hello there my default prefix is %s and you can view all commands using **%shelp**", PREFIX, PREFIX)).queue();
            else return;
        if (event.getMember() != null) {
            Guild guild = event.getGuild();
            User author = event.getAuthor();
            Helpers.guildCount = event.getJDA().asBot().getShardManager().getGuilds().size();
            String content = event.getMessage().getContentRaw();
            for (Message.Attachment a : event.getMessage().getAttachments()) {
                content += "\n" + a.getUrl();
            }
            String finalContent = content;
            TaskScheduler.async(() -> mySQL.createMessage(event.getMessageIdLong(), finalContent, author.getIdLong(), guild.getIdLong(), event.getChannel().getIdLong()));
            if (event.getMessage().getContentRaw().equalsIgnoreCase(guild.getSelfMember().getAsMention())) {
                String prefix = SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong());
                event.getChannel().sendMessage(String.format(("Hello there my default prefix is %s " + (prefix.equals(PREFIX) ? "" : String.format("\nThis server has configured %s as the prefix\n", prefix)) + "and you can view all commands using **%shelp**"), PREFIX, prefix)).queue();
            }
            if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                TaskScheduler.async(() -> {
                    String message = event.getMessage().getContentRaw();
                    String detectedWord = null;
                    HashMap<Integer, Integer> deniedPositions = new HashMap<>();
                    HashMap<Integer, Integer> allowedPositions = new HashMap<>();
                    List<String> deniedList = mySQL.getFilters(guild.getIdLong(), "denied");
                    List<String> allowedList = mySQL.getFilters(guild.getIdLong(), "allowed");
                    addPositions(message, deniedPositions, deniedList);
                    addPositions(message, allowedPositions, allowedList);

                    if (allowedPositions.size() > 0 && deniedPositions.size() > 0) {
                        for (Integer beginDenied : deniedPositions.keySet()) {
                            Integer endDenied = deniedPositions.get(beginDenied);
                            for (Integer beginAllowed : allowedPositions.keySet()) {
                                Integer endAllowed = allowedPositions.get(beginAllowed);
                                if (beginDenied < beginAllowed || endDenied > endAllowed) {
                                    detectedWord = message.substring(beginDenied, endDenied);
                                }
                            }
                        }
                    } else if (deniedPositions.size() > 0) {
                        detectedWord = "";
                        for (Integer beginDenied : deniedPositions.keySet()) {
                            Integer endDenied = deniedPositions.get(beginDenied);
                            detectedWord += message.substring(beginDenied, endDenied) + ", ";
                        }
                    }
                    if (detectedWord != null) {
                        MessageHelper.filteredMessageDeleteCause.put(event.getMessageIdLong(), detectedWord.substring(0, detectedWord.length() - 2));
                        event.getMessage().delete().reason("Use of prohibited words").queue();
                    }
                });
            }
        }

        if (SetVerificationChannel.verificationChannelsCache.getUnchecked(event.getGuild().getIdLong()) == event.getChannel().getIdLong()) {
            try {
                if (SetVerificationCode.verificationCodeCache.get(event.getGuild().getIdLong()) != null) {
                    if (event.getMessage().getContentRaw().equalsIgnoreCase(SetVerificationCode.verificationCodeCache.get(event.getGuild().getIdLong()))) {
                        event.getMessage().delete().reason("Verification Channel").queue(s -> MessageHelper.botDeletedMessages.add(event.getMessageIdLong()));
                        JoinLeave.verify(event.getGuild(), event.getAuthor());
                        removeMemberFromTriesCache(event);
                    } else if (SetVerificationThreshold.verificationThresholdCache.get(event.getGuild().getIdLong()) != 0) {
                        event.getMessage().delete().reason("Verification Channel").queue(s -> MessageHelper.botDeletedMessages.add(event.getMessageIdLong()));
                        if (guildUserVerifyTries.containsKey(event.getGuild().getIdLong())) {
                            if (guildUserVerifyTries.get(event.getGuild().getIdLong()).containsKey(event.getAuthor().getIdLong())) {
                                HashMap<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
                                userTriesBuffer.replace(event.getAuthor().getIdLong(), userTriesBuffer.get(event.getAuthor().getIdLong()) + 1);
                                guildUserVerifyTries.replace(event.getGuild().getIdLong(), userTriesBuffer);
                            } else {
                                HashMap<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
                                userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                                guildUserVerifyTries.replace(event.getGuild().getIdLong(), userTriesBuffer);
                            }
                        } else {
                            HashMap<Long, Integer> userTriesBuffer = new HashMap<>();
                            userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                            guildUserVerifyTries.put(event.getGuild().getIdLong(), userTriesBuffer);
                        }
                        if (guildUserVerifyTries.get(event.getGuild().getIdLong()).get(event.getAuthor().getIdLong()) == SetVerificationThreshold.verificationThresholdCache.get(event.getGuild().getIdLong())) {
                            event.getGuild().getController().kick(event.getMember()).reason("Failed verification").queue();
                            removeMemberFromTriesCache(event);
                        }
                    } else {
                        event.getMessage().delete().reason("Verification Channel").queue(s -> MessageHelper.botDeletedMessages.add(event.getMessageIdLong()));
                    }
                }
            } catch (ExecutionException | CacheLoader.InvalidCacheLoadException ignored) {
            }
        }
    }

    private void addPositions(String message, HashMap<Integer, Integer> deniedPositions, List<String> deniedList) {
        for (String toFind : deniedList) {
            Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
            Matcher match = word.matcher(message.toLowerCase());
            while (match.find()) {
                if (deniedPositions.keySet().contains(match.start()) && deniedPositions.get(match.start()) < match.end())
                    deniedPositions.replace(match.start(), match.end());
                else deniedPositions.put(match.start(), match.end());
            }
        }
    }

    private void removeMemberFromTriesCache(GuildMessageReceivedEvent event) {
        if (guildUserVerifyTries.containsKey(event.getGuild().getIdLong())) {
            HashMap<Long, Integer> memberTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
            memberTriesBuffer.remove(event.getAuthor().getIdLong());
            if (memberTriesBuffer.size() > 0) guildUserVerifyTries.put(event.getGuild().getIdLong(), memberTriesBuffer);
            else guildUserVerifyTries.remove(event.getGuild().getIdLong());
        }
    }


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        if (Helpers.lastRunTimer1 < (System.currentTimeMillis() - 4_000))
            Helpers.startTimer(event.getJDA(), 1);
        if (Helpers.lastRunTimer2 < (System.currentTimeMillis() - 61_000))
            Helpers.startTimer(event.getJDA(), 2);
        if (Helpers.lastRunTimer3 < (System.currentTimeMillis() - 1_810_000))
            Helpers.startTimer(event.getJDA(), 3);
        Guild guild = event.getGuild();
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS) && (SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong()) != -1)) {
            JSONObject message = mySQL.getMessageObject(event.getMessageIdLong());
            if (message.keySet().contains("authorId"))
                event.getJDA().retrieveUserById(message.getLong("authorId")).queue(author -> {
                    if (author != null && !black.contains(author)) {
                        guild.getBanList().queue(bans -> {
                            if (bans.stream().map(Ban::getUser).anyMatch(author::equals)) {
                                black.add(author);
                                return;
                            }
                            doMessageDeleteChecks(event, guild, message, author);
                            mySQL.executeUpdate("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
                        });
                    }
                });
        }
    }

    private void doMessageDeleteChecks(GuildMessageDeleteEvent event, Guild guild, JSONObject message, User author) {
        guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).queue(auditLogEntries -> {
            if (auditLogEntries.size() == 0) return;
            AuditLogEntry auditLogEntry = auditLogEntries.get(0);
            String t = auditLogEntry.getOption(AuditLogOption.COUNT);
            if (t != null) {
                boolean sameAsLast = latestId.equals(auditLogEntry.getId()) && latestChanges != Integer.valueOf(t);
                latestId = auditLogEntry.getId();
                latestChanges = Integer.valueOf(t);
                ZonedDateTime deletionTime = MiscUtil.getCreationTime(auditLogEntry.getIdLong()).toZonedDateTime();
                ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(deletionTime.getOffset());
                deletionTime = deletionTime.plusSeconds(1).plusNanos((event.getJDA().getPing() * 1_000_000));

                boolean split = false;
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Message deleted in #" + event.getChannel().getName() + MessageHelper.spaces.substring(0, 45 + author.getName().length()) + "\u200B");
                eb.setThumbnail(author.getEffectiveAvatarUrl());
                eb.setColor(Color.decode("#000001"));
                if (message.getString("content").length() > 1850) {
                    eb.setDescription("```LDIF" + "\nSender: " + author.getName() + "#" + author.getDiscriminator() + "\nMessage part 1: " + message.getString("content").substring(0, 1500) + "```");
                    split = true;
                } else
                    eb.setDescription("```LDIF" + "\nSender: " + author.getName() + "#" + author.getDiscriminator() + "\nMessage: " + message.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSenderID: " + author.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
                if (MessageHelper.filteredMessageDeleteCause.keySet().contains(event.getMessageIdLong()) &&
                        guild.getTextChannelById(SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                    // Filtered Message log
                    eb.setColor(Color.ORANGE);
                    TextChannel fmLogChannel = guild.getTextChannelById(SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong()));
                    sendSplitIfNeeded(message, author, split, eb, fmLogChannel);
                    eb.addField("Detected: ", "`" + MessageHelper.filteredMessageDeleteCause.get(event.getMessageIdLong()).replaceAll("`", "´") + "`", false);
                    User bot = event.getJDA().getSelfUser();
                    eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getEffectiveAvatarUrl());
                    fmLogChannel.sendMessage(eb.build()).queue();
                    MessageHelper.filteredMessageDeleteCause.remove(event.getMessageIdLong());
                } else if (MessageHelper.purgedMessageDeleter.containsKey(event.getMessageIdLong()) &&
                        guild.getTextChannelById(SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                    eb.setColor(Color.decode("#551A8B"));
                    User purger = event.getJDA().asBot().getShardManager().getUserById(MessageHelper.purgedMessageDeleter.get(event.getMessageIdLong()));
                    TextChannel pmLogChannel = guild.getTextChannelById(SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong()));
                    sendSplitIfNeeded(message, author, split, eb, pmLogChannel);
                    if (purger != null)
                        eb.setFooter("Purged by: " + purger.getName() + "#" + purger.getDiscriminator(), purger.getEffectiveAvatarUrl());
                    pmLogChannel.sendMessage(eb.build()).queue();
                    MessageHelper.purgedMessageDeleter.remove(event.getMessageIdLong());
                } else if (MessageHelper.botDeletedMessages.remove(event.getMessageIdLong())) {
                    User deleter = event.getJDA().getSelfUser();
                    log(guild, author, eb, deleter, message, split);
                } else if (now.toInstant().toEpochMilli() - deletionTime.toInstant().toEpochMilli() < 1000) {
                    User deleter = auditLogEntry.getUser();
                    log(guild, author, eb, deleter, message, split);
                } else {
                    User deleter = sameAsLast ? auditLogEntry.getUser() : event.getJDA().asBot().getShardManager().getUserById(Melijn.mySQL.getMessageAuthorId(event.getMessageIdLong()));
                    log(guild, author, eb, deleter, message, split);
                }
        }
        });
    }

    private void sendSplitIfNeeded(JSONObject message, User author, boolean split, EmbedBuilder eb, TextChannel pmLogChannel) {
        if (split) {
            eb.setTitle(eb.build().getTitle() + " part 1");
            pmLogChannel.sendMessage(eb.build()).queue();
            eb.setDescription("```LDIF\npart 2: " + message.getString("content").substring(1850) + "\nSenderID: " + author.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
            eb.setTitle("part 2");
            eb.setThumbnail("https://melijn.com/files/u/03-09-2018--09.16-29s.png");
        }
    }

    private void log(Guild guild, User author, EmbedBuilder eb, User deleter, JSONObject message, boolean split) {
        if (deleter != null) {
            if (split) {
                eb.setTitle(eb.build().getTitle() + " part 1");
                if (author.equals(deleter) && guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                    guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong())).sendMessage(eb.build()).queue();
                } else if (guild.getTextChannelById(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                    guild.getTextChannelById(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong())).sendMessage(eb.build()).queue();
                }
                eb.setTitle("part 2");
                eb.setThumbnail("https://melijn.com/files/u/03-09-2018--09.16-29s.png");
                eb.setDescription("```LDIF\npart 2: " + message.getString("content").substring(1500) + "\nSenderID: " + author.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
            }
            eb.setFooter("Deleted by: " + deleter.getName() + "#" + deleter.getDiscriminator(), deleter.getEffectiveAvatarUrl());
            if (author.equals(deleter) && guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong())).sendMessage(eb.build()).queue();
            } else if (guild.getTextChannelById(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                guild.getTextChannelById(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong())).sendMessage(eb.build()).queue();
            }
        }
    }
}
