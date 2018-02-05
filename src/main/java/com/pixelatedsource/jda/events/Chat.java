package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.db.MySQL;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Chat extends ListenerAdapter {

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private MySQL mySQL = PixelSniper.mySQL;

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        User author = event.getAuthor();
        Member member = event.getMember();
        try {
            mySQL.createMessage(event.getMessageId(), event.getMessage().getContentRaw(), author.getId(), guild.getId(), event.getChannel().getId());
            ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM active_bans WHERE guildId='" + guild.getId() + "' AND endTime < " + System.currentTimeMillis());
            while (rs.next()) {
                event.getGuild().getController().unban(rs.getString("victimId")).queue();
                PixelSniper.mySQL.update("DELETE FROM active_bans WHERE guildId='" + guild.getId() + "' AND victimId='" + rs.getString("victimId") + "' AND endTime < " + System.currentTimeMillis());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent e) {
        Guild guild = e.getGuild();
        ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM history_messages WHERE messageId= '" + e.getMessageId() + "' ");
        try {
            if (rs.next()) {
                executorService.execute(() -> {
                    try {
                        User user = e.getJDA().retrieveUserById(rs.getString("authorId")).complete();
                        if (user != null) {
                            for (Guild.Ban ban : guild.getBanList().complete()) {
                                if (ban.getUser() == user) return;
                            }
                            OffsetDateTime tijdJ = OffsetDateTime.from(OffsetDateTime.now().atZoneSameInstant(OffsetDateTime.now().getOffset())).minusNanos(1_800_000_000 + (e.getJDA().getPing() * 1_000_000));
                            AuditLogEntry auditLogEntry = guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).complete().get(0);
                            OffsetDateTime tijdD = OffsetDateTime.from(auditLogEntry.getCreationTime().atZoneSameInstant(tijdJ.getOffset()));
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Message deleted in #" + e.getChannel().getName());
                            eb.setThumbnail(user.getAvatarUrl());
                            {
                                long millisTijdJ = 0;
                                millisTijdJ += tijdJ.getYear() * 31_556_952_000L;
                                millisTijdJ += tijdJ.getMonthValue() * 2_629_746_000L;
                                millisTijdJ += tijdJ.getDayOfMonth() * 86_400_000L;
                                millisTijdJ += tijdJ.getHour() * 3_600_000L;
                                millisTijdJ += tijdJ.getMinute() * 60_000L;
                                millisTijdJ += tijdJ.getSecond() * 1_000L;
                                millisTijdJ += tijdJ.getNano() / 1_000_000L;
                                long millisTijdD = 0;
                                millisTijdD += tijdD.getYear() * 31_556_952_000L;
                                millisTijdD += tijdD.getMonthValue() * 2_629_746_000L;
                                millisTijdD += tijdD.getDayOfMonth() * 86_400_000L;
                                millisTijdD += tijdD.getHour() * 3_600_000L;
                                millisTijdD += tijdD.getMinute() * 60_000L;
                                millisTijdD += tijdD.getSecond() * 1_000L;
                                millisTijdD += tijdD.getNano() / 1_000_000L;
                                System.out.println(Math.abs(millisTijdJ - millisTijdD));
                            }
                            /*
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            eb.setColor(Color.decode("#000001"));
                            eb.setDescription("```LDIF" +
                                    "\nSender: " + user.getName() + "#" + user.getDiscriminator() +
                                    "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") +
                                    "\nSender's Id: " + rs.getString("authorId") +
                                    "\nSent Time: " + MessageHelper.millisToDate(rs.getLong("sentTime")) +
                                    "```");
                            if (MessageHelper.filterDeletedMessages.get(e.getMessageId()) != null) {
                                eb.addField("Detected: ", "`" + MessageHelper.filterDeletedMessages.get(e.getMessageId()).replaceAll("`", "´") + "`", false);
                                eb.setColor(Color.ORANGE);
                                MessageHelper.filterDeletedMessages.remove(e.getMessageId());
                            } else if (MessageHelper.deletedByEmote.get(e.getMessageId()) != null) {
                                User staff = MessageHelper.deletedByEmote.get(e.getMessageId());
                                eb.addField("Deleted by: ", staff.getName() + "#" + staff.getDiscriminator(), false);
                                eb.setColor(Color.magenta);
                            }
                            guild.getTextChannelById(mySQL.getChannelId(e.getGuild().getId(), ChannelType.LOG)).sendMessage(eb.build()).queue(v -> {
                                if (eb.build().getColor().equals(Color.decode("#000001"))) mySQL.addUnclaimed(e.getMessageId(), v.getId());
                            });
                            mySQL.update("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
                            mySQL.saveDeletedMessage(e.getMessageId());
                            */
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
