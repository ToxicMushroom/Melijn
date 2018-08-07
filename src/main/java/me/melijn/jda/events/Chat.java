package me.melijn.jda.events;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.SetLogChannelCommand;
import me.melijn.jda.commands.management.SetVerificationChannel;
import me.melijn.jda.commands.management.SetVerificationCode;
import me.melijn.jda.commands.management.SetVerificationThreshold;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogOption;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.Ban;
import net.dv8tion.jda.core.entities.Message;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends ListenerAdapter {

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static List<User> black = new ArrayList<>();
    private MySQL mySQL = Melijn.mySQL;
    private String latestId = "";
    private int latestChanges = 0;
    HashMap<Long, HashMap<Long, Integer>> guildUserVerifyTries = new HashMap<>();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong())) return;
        if (event.getMember() != null) {
            Guild guild = event.getGuild();
            User author = event.getAuthor();
            Helpers.guildCount = event.getJDA().asBot().getShardManager().getGuilds().size();
            String content = event.getMessage().getContentRaw();
            for (Message.Attachment a : event.getMessage().getAttachments()) {
                content += "\n" + a.getUrl();
            }
            String finalContent = content;
            Melijn.MAIN_THREAD.submit(() -> mySQL.createMessage(event.getMessageIdLong(), finalContent, author.getIdLong(), guild.getIdLong(), event.getChannel().getIdLong()));

            if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                Melijn.MAIN_THREAD.submit(() ->  {
                    String message = event.getMessage().getContentRaw();
                    String detectedWord = null;
                    HashMap<Integer, Integer> deniedPositions = new HashMap<>();
                    HashMap<Integer, Integer> allowedPositions = new HashMap<>();
                    List<String> deniedList = mySQL.getFilters(guild.getIdLong(), "denied");
                    List<String> allowedList = mySQL.getFilters(guild.getIdLong(), "allowed");

                    for (String toFind : deniedList) {
                        Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
                        Matcher match = word.matcher(message.toLowerCase());
                        while (match.find()) {
                            if (deniedPositions.keySet().contains(match.start()) && deniedPositions.get(match.start()) < match.end())
                                deniedPositions.replace(match.start(), match.end());
                            else deniedPositions.put(match.start(), match.end());
                        }
                    }

                    for (String toFind : allowedList) {
                        Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
                        Matcher match = word.matcher(message.toLowerCase());
                        while (match.find()) {
                            if (allowedPositions.keySet().contains(match.start()) && allowedPositions.get(match.start()) < match.end())
                                allowedPositions.replace(match.start(), match.end());
                            else allowedPositions.put(match.start(), match.end());
                        }
                    }

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
                        MessageHelper.filterDeletedMessages.put(event.getMessageId(), detectedWord.substring(0, detectedWord.length() - 2));
                        event.getMessage().delete().reason("Use of prohibited words").queue();
                    }
                });
            }
        }

        if (SetVerificationChannel.verificationChannels.values().contains(event.getChannel().getIdLong())) {
            if (SetVerificationCode.guildCodes.containsKey(event.getGuild().getIdLong())) {
                if (event.getMessage().getContentRaw().equalsIgnoreCase(SetVerificationCode.guildCodes.get(event.getGuild().getIdLong()))) {
                    event.getMessage().delete().queue();
                    JoinLeave.verify(event.getGuild(), event.getAuthor());
                    if (SetVerificationThreshold.guildVerificationThresholds.containsKey(event.getGuild().getIdLong()) && guildUserVerifyTries.containsKey(event.getGuild().getIdLong())) {
                        HashMap<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
                        userTriesBuffer.remove(event.getAuthor().getIdLong());
                        guildUserVerifyTries.replace(event.getGuild().getIdLong(), userTriesBuffer);
                    }
                } else if (SetVerificationThreshold.guildVerificationThresholds.containsKey(event.getGuild().getIdLong())) {
                    event.getMessage().delete().queue();
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
                    if (guildUserVerifyTries.get(event.getGuild().getIdLong()).get(event.getAuthor().getIdLong()) == SetVerificationThreshold.guildVerificationThresholds.get(event.getGuild().getIdLong())) {
                        event.getGuild().getController().kick(event.getMember()).reason("Failed verification").queue();
                        if (SetVerificationThreshold.guildVerificationThresholds.containsKey(event.getGuild().getIdLong()) && guildUserVerifyTries.containsKey(event.getGuild().getIdLong())) {
                            HashMap<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
                            userTriesBuffer.remove(event.getAuthor().getIdLong());
                            guildUserVerifyTries.replace(event.getGuild().getIdLong(), userTriesBuffer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong())) return;
        if (Helpers.lastRunTimer1 < (System.currentTimeMillis() - 4_000))
            Helpers.startTimer(event.getJDA(), Melijn.dblAPI, 1);
        if (Helpers.lastRunTimer2 < (System.currentTimeMillis() - 61_000))
            Helpers.startTimer(event.getJDA(), Melijn.dblAPI, 2);
        if (Helpers.lastRunTimer3 < (System.currentTimeMillis() - 1_810_000))
            Helpers.startTimer(event.getJDA(), Melijn.dblAPI, 3);
        Guild guild = event.getGuild();
        if ((SetLogChannelCommand.pmLogChannelMap.containsKey(guild.getIdLong()) ||
                SetLogChannelCommand.odmLogChannelMap.containsKey(guild.getIdLong()) ||
                SetLogChannelCommand.sdmLogChannelMap.containsKey(guild.getIdLong()) ||
                SetLogChannelCommand.fmLogChannelMap.containsKey(guild.getIdLong())) && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            executorService.execute(() -> {
                JSONObject message = mySQL.getMessageObject(event.getMessageIdLong());
                User user = event.getJDA().retrieveUserById(message.getLong("authorId")).complete();
                if (user != null && !black.contains(user)) {
                    if (guild.getBanList().complete().stream().map(Ban::getUser).anyMatch(user::equals)) {
                        black.add(user);
                        return;
                    }
                    AuditLogEntry auditLogEntry = guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).complete().get(0);
                    String t = auditLogEntry.getOption(AuditLogOption.COUNT);
                    if (t != null) {
                        boolean sameAsLast = latestId.equals(auditLogEntry.getId()) && latestChanges != Integer.valueOf(t);
                        latestId = auditLogEntry.getId();
                        latestChanges = Integer.valueOf(t);
                        ZonedDateTime deletionTime = MiscUtil.getCreationTime(auditLogEntry.getIdLong()).toZonedDateTime();
                        ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(deletionTime.getOffset());
                        deletionTime = deletionTime.plusSeconds(1).plusNanos((event.getJDA().getPing() * 1_000_000));

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Message deleted in #" + event.getChannel().getName());
                        eb.setThumbnail(user.getEffectiveAvatarUrl());
                        eb.setColor(Color.decode("#000001"));
                        eb.setDescription("```LDIF" + "\nSender: " + user.getName() + "#" + user.getDiscriminator() + "\nMessage: " + message.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSender's Id: " + user.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
                        if (MessageHelper.filterDeletedMessages.get(event.getMessageId()) != null) {
                            eb.addField("Detected: ", "`" + MessageHelper.filterDeletedMessages.get(event.getMessageId()).replaceAll("`", "´") + "`", false);
                            eb.setColor(Color.ORANGE);
                            User bot = event.getJDA().getSelfUser();
                            eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getEffectiveAvatarUrl());
                            MessageHelper.filterDeletedMessages.remove(event.getMessageId());
                            if (SetLogChannelCommand.fmLogChannelMap.containsKey(guild.getIdLong())) {
                                guild.getTextChannelById(SetLogChannelCommand.fmLogChannelMap.get(guild.getIdLong())).sendMessage(eb.build()).queue();
                            }
                        } else if (now.toInstant().toEpochMilli() - deletionTime.toInstant().toEpochMilli() < 1000) {
                            User deletor = auditLogEntry.getUser();
                            log(guild, user, eb, deletor);
                        } else if (MessageHelper.purgedMessages.get(event.getMessageId()) != null) {
                            User purger = MessageHelper.purgedMessages.get(event.getMessageId());
                            eb.setColor(Color.decode("#551A8B"));
                            eb.setFooter("Purged by: " + purger.getName() + "#" + purger.getDiscriminator(), purger.getEffectiveAvatarUrl());
                            MessageHelper.purgedMessages.remove(event.getMessageId());
                            if (SetLogChannelCommand.pmLogChannelMap.containsKey(guild.getIdLong())) {
                                guild.getTextChannelById(SetLogChannelCommand.pmLogChannelMap.get(guild.getIdLong())).sendMessage(eb.build()).queue();
                            }
                        } else {
                            User deletor = sameAsLast ? auditLogEntry.getUser() : event.getJDA().getUserById(Melijn.mySQL.getMessageAuthorId(event.getMessageIdLong()));
                            log(guild, user, eb, deletor);
                        }

                    }
                    Melijn.MAIN_THREAD.submit(() -> mySQL.update("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L)));
                }
            });
        }
    }

    private void log(Guild guild, User user, EmbedBuilder eb, User deletor) {
        if (deletor != null) {
            eb.setFooter("Deleted by: " + deletor.getName() + "#" + deletor.getDiscriminator(), deletor.getEffectiveAvatarUrl());
            if (user == deletor && SetLogChannelCommand.sdmLogChannelMap.containsKey(guild.getIdLong())) {
                guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelMap.get(guild.getIdLong())).sendMessage(eb.build()).queue();
            } else if (SetLogChannelCommand.odmLogChannelMap.containsKey(guild.getIdLong())) {
                guild.getTextChannelById(SetLogChannelCommand.odmLogChannelMap.get(guild.getIdLong())).sendMessage(eb.build()).queue();
            }
        }
    }
}
