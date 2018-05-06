package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogOption;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.Ban;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private MySQL mySQL = PixelSniper.mySQL;
    private String latestId = "";
    private int latestChanges = 0;

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event != null && event.getMember() != null) {
            Guild guild = event.getGuild();
            User author = event.getAuthor();
            try {
                mySQL.createMessage(event.getMessageId(), event.getMessage().getContentRaw(), author.getId(), guild.getId(), event.getChannel().getId());

                if (!event
                        .getMember()
                        .hasPermission(
                                Permission
                                        .MESSAGE_MANAGE)) {
                    String message = event.getMessage().getContentRaw();
                    String detectedWord = null;
                    HashMap<Integer, Integer> deniedPositions = new HashMap<>();
                    HashMap<Integer, Integer> allowedPositions = new HashMap<>();
                    List<String> deniedList = mySQL.getFilters(guild, "denied");
                    List<String> allowedList = mySQL.getFilters(guild, "allowed");

                    for (String toFind : deniedList) {
                        Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
                        Matcher match = word.matcher(message.toLowerCase());
                        while (match.find()) {
                            if (deniedPositions.keySet().contains(match.start()) && deniedPositions.get(match.start()) < match.end()) deniedPositions.replace(match.start(), match.end());
                            else deniedPositions.put(match.start(), match.end());
                        }
                    }
                    for (String toFind : allowedList) {
                        Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
                        Matcher match = word.matcher(message.toLowerCase());
                        while (match.find()) {
                            if (allowedPositions.keySet().contains(match.start()) && allowedPositions.get(match.start()) < match.end()) allowedPositions.replace(match.start(), match.end());
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
                }

                ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM active_bans WHERE guildId='" + guild.getId() + "' AND endTime < " + System.currentTimeMillis());
                while (rs.next()) {
                    event.getGuild().getController().unban(rs.getString("victimId")).queue();
                    PixelSniper.mySQL.update("DELETE FROM active_bans WHERE guildId='" + guild.getId() + "' AND victimId='" + rs.getString("victimId") + "' AND endTime < " + System.currentTimeMillis());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (Helpers.lastRunMillis < System.currentTimeMillis() - 60_000) Helpers.startTimer(event.getJDA(), PixelSniper.dblAPI);
        Guild guild = event.getGuild();
        ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM history_messages WHERE messageId= '" + event.getMessageId() + "' ");
        try {
            if (rs.next()) {
                executorService.execute(() -> {
                    try {
                        User user = event.getJDA().retrieveUserById(rs.getString("authorId")).complete();
                        if (user != null && !black.contains(user)) {
                            if (guild.getBanList().complete().stream().map(Ban::getUser).anyMatch(user::equals)) {
                                black.add(user);
                                return;
                            }
                            AuditLogEntry auditLogEntry = guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).complete().get(0);
                            boolean sameAsLast = latestId.equals(auditLogEntry.getId()) && latestChanges != Integer.valueOf(auditLogEntry.getOption(AuditLogOption.COUNT));
                            latestId = auditLogEntry.getId();
                            latestChanges = Integer.valueOf(auditLogEntry.getOption(AuditLogOption.COUNT));
                            ZonedDateTime deletionTime = MiscUtil.getCreationTime(auditLogEntry.getIdLong()).toZonedDateTime();
                            ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(deletionTime.getOffset());
                            deletionTime = deletionTime.plusSeconds(1).plusNanos((event.getJDA().getPing() * 1_000_000));

                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Message deleted in #" + event.getChannel().getName());
                            eb.setThumbnail(user.getAvatarUrl());
                            eb.setColor(Color.decode("#000001"));
                            eb.setDescription("```LDIF" + "\nSender: " + user.getName() + "#" + user.getDiscriminator() + "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSender's Id: " + rs.getString("authorId") + "\nSent Time: " + MessageHelper.millisToDate(rs.getLong("sentTime")) + "```");
                            if (MessageHelper.filterDeletedMessages.get(event.getMessageId()) != null) {
                                eb.addField("Detected: ", "`" + MessageHelper.filterDeletedMessages.get(event.getMessageId()).replaceAll("`", "´") + "`", false);
                                eb.setColor(Color.ORANGE);
                                User bot = event.getJDA().getSelfUser();
                                eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getAvatarUrl());
                                MessageHelper.filterDeletedMessages.remove(event.getMessageId());
                            } else if (now.toInstant().toEpochMilli() - deletionTime.toInstant().toEpochMilli() < 1000) {
                                User deletor = auditLogEntry.getUser();
                                if (deletor != null) eb.setFooter("Deleted by: " + deletor.getName() + "#" + deletor.getDiscriminator(), deletor.getAvatarUrl());
                            } else if (MessageHelper.purgedMessages.get(event.getMessageId()) != null) {
                                User purger = MessageHelper.purgedMessages.get(event.getMessageId());
                                eb.setColor(Color.decode("#551A8B"));
                                eb.setFooter("Purged by: " + purger.getName() + "#" + purger.getDiscriminator(), purger.getAvatarUrl());
                                MessageHelper.purgedMessages.remove(event.getMessageId());
                            } else {
                                User deletor = sameAsLast ? auditLogEntry.getUser() : PixelSniper.mySQL.getMessageAuthor(event.getMessageId(), event.getJDA());
                                if (deletor != null) eb.setFooter("Deleted by: " + deletor.getName() + "#" + deletor.getDiscriminator(), deletor.getAvatarUrl());
                            }

                            guild.getTextChannelById(mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG)).sendMessage(eb.build()).queue(v -> {
                                if (eb.build().getColor().equals(Color.decode("#000001"))) mySQL.addUnclaimed(event.getMessageId(), v.getId());
                            });
                            mySQL.update("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
                            mySQL.saveDeletedMessage(event.getMessageId());
                        }
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                });
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

    }
}
