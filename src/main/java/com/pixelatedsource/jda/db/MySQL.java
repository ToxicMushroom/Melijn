package com.pixelatedsource.jda.db;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.RoleType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.pixelatedsource.jda.utils.MessageHelper.millisToDate;

public class MySQL {

    private String ip;
    private String pass;
    private String user;
    private String dbname;
    private static Connection con;

    public MySQL(String ip, String user, String pass, String dbname) {
        this.ip = ip;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        connect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + this.ip + ":3306/" + this.dbname + "?autoReconnect=true&useUnicode=true&useSSL=false", this.user, this.pass);
            Statement statement = con.createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
            System.out.println("[MySQL] has connected");
            update("CREATE TABLE IF NOT EXISTS mute_roles(guildId varchar(128), roleId varchar(128));");
            update("CREATE TABLE IF NOT EXISTS commands(commandName varchar(1000), gebruik varchar(1000), description varchar(2000), extra varchar(2000), category varchar(100), aliases varchar(200));");
            update("CREATE TABLE IF NOT EXISTS stream_urls(guildId varchar(127), url varchar(1500))");
            update("CREATE TABLE IF NOT EXISTS prefixes(guildId varchar(128), prefix varchar(128));");
            update("CREATE TABLE IF NOT EXISTS perms(guildName varchar(64), guildId varchar(128), roleName varchar(64), roleId varchar(128), permission varchar(256));");
            update("CREATE TABLE IF NOT EXISTS log_channels(guildId varchar(128), channelId varchar(128))");
            update("CREATE TABLE IF NOT EXISTS music_channels(guildId varchar(128), channelId varchar(128))");
            update("CREATE TABLE IF NOT EXISTS streamer_modes(guildId varchar(128), state boolean)");
            update("CREATE TABLE IF NOT EXISTS warns(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), moment bigint);");
            update("CREATE TABLE IF NOT EXISTS active_bans(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint);");
            update("CREATE TABLE IF NOT EXISTS history_bans(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            update("CREATE TABLE IF NOT EXISTS active_mutes(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint);");
            update("CREATE TABLE IF NOT EXISTS history_mutes(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            update("CREATE TABLE IF NOT EXISTS history_messages(guildId varchar(128), authorId varchar(128), messageId varchar(128), content varchar(3000), textChannelId varchar(128), sentTime bigint);");
            update("CREATE TABLE IF NOT EXISTS deleted_messages(guildId varchar(128), authorId varchar(128), messageId varchar(128), content varchar(3000), textChannelId varchar(128), sentTime bigint, delTime bigint);");
            update("CREATE TABLE IF NOT EXISTS unclaimed_messages(deletedMessageId varchar(64), logMessageId varchar(64));");
        } catch (SQLException e) {
            System.out.println((char) 27 + "[31m" + "did not connect");
            System.exit(44);
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (con != null) {
                con.close();
                System.out.println("[MySQL] has disconnected");
            }
        } catch (SQLException e) {
            System.out.println("[MySQL] did not disconnect proparily error:" + e.getMessage());
        }
    }

    public void update(String qry) {
        try {
            Statement st = con.createStatement();
            st.executeUpdate(qry);
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet query(String qry) {
        ResultSet rs = null;
        try {
            Statement st = con.createStatement();
            rs = st.executeQuery(qry);
        } catch (SQLException e) {
            connect();
            e.printStackTrace();
        }
        return rs;
    }

    //Message stuff----------------------------------------------------
    private boolean messageExists(String id) {
        try {
            ResultSet rs = query("SELECT * FROM history_messages WHERE messageId= '" + id + "'");
            return rs.next() && rs.getString("messageId") != null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public User getMessageAuthor(String messageId, JDA jda) {
        if (messageExists(messageId)) {
            try {
                PreparedStatement getAuthor = con.prepareStatement("SELECT * FROM history_messages WHERE messageId= ?");
                getAuthor.setString(1, messageId);
                ResultSet rs = getAuthor.executeQuery();

                while (rs.next()) {
                    return jda.retrieveUserById(rs.getString("authorId")).complete();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void createMessage(String messageId, String content, String authorId, String guildId, String textchannelid) {
        if (!messageExists(messageId)) {
            try {
                PreparedStatement createMessage = con.prepareStatement("INSERT INTO history_messages(guildId, authorId, messageId, content, textChannelId, sentTime) VALUES (?, ?, ?, ?, ?, ?)");
                createMessage.setString(1, guildId);
                createMessage.setString(2, authorId);
                createMessage.setString(3, messageId);
                createMessage.setString(4, content);
                createMessage.setString(5, textchannelid);
                createMessage.setLong(6, System.currentTimeMillis());
                createMessage.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveDeletedMessage(String messageid) {
        ResultSet rs = query("SELECT * FROM history_messages WHERE messageId= '" + messageid + "'");
        try {
            if (rs.next()) {
                String guildId = rs.getString("guildId");
                String authorId = rs.getString("authorId");
                String content = rs.getString("content");
                String channelId = rs.getString("textChannelId");
                String sentTime = rs.getString("sentTime");
                PreparedStatement createMessage = con.prepareStatement("INSERT INTO deleted_messages (guildId, authorId, messageId, content, textChannelId, sentTime, delTime) VALUES (?, ?, ?, ?, ?, ?, ?)");
                createMessage.setString(1, guildId);
                createMessage.setString(2, authorId);
                createMessage.setString(3, messageid);
                createMessage.setString(4, content);
                createMessage.setString(5, channelId);
                createMessage.setString(6, sentTime);
                createMessage.setLong(7, System.currentTimeMillis());
                createMessage.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUnclaimed(String deletedmessageid, String unclaimedid) {
        try {
            PreparedStatement addunclaimed = con.prepareStatement("INSERT INTO unclaimed_messages (deletedMessageId, logMessageId) VALUES (?, ?)");
            addunclaimed.setString(1, deletedmessageid);
            addunclaimed.setString(2, unclaimedid);
            addunclaimed.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getMessageIdByUnclaimedId(String unclaimedid) {
        try {
            PreparedStatement getMessageId = con.prepareStatement("SELECT * FROM unclaimed_messages WHERE logMessageId= ?");
            getMessageId.setString(1, unclaimedid);
            ResultSet rs = getMessageId.executeQuery();
            String s = null;
            while (rs.next()) {
                s = rs.getString("deletedMessageId");
            }
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MessageEmbed unclaimedToClaimed(String messageid, JDA jda, User staff) {
        try {
            PreparedStatement getdeletedmessage = con.prepareStatement("SELECT * FROM deleted_messages WHERE messageId= ?");
            getdeletedmessage.setString(1, messageid);
            ResultSet rs = getdeletedmessage.executeQuery();
            if (rs.next()) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Message deleted in #" + jda.getTextChannelById(rs.getString("textChannelId")).getName());
                eb.setThumbnail(jda.retrieveUserById(rs.getString("authorId")).complete().getAvatarUrl());
                eb.setFooter(millisToDate(rs.getLong("delTime")), Helpers.getFooterIcon());
                eb.setColor(Color.magenta);
                String authorId = rs.getString("authorId");
                User author = jda.retrieveUserById(authorId).complete();
                eb.setDescription("```LDIF" + "\nSender: " + author.getName() + "#" + author.getDiscriminator() + "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSender's ID: " + rs.getString("authorId") + "\nSent Time: " + millisToDate(rs.getLong("sentTime")) + "```");
                eb.addField("Deleted by: ", staff.getName() + "#" + staff.getDiscriminator(), false);
                return eb.build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Permissions stuff---------------------------------------------------------
    public void addPermission(Guild guild, Role role, String permission) {
        String id = role == null ? guild.getRolesByName("@everyone", false).get(0).getId() : role.getId();
        String name = role == null ? "@everyone" : role.getName();

        try {
            PreparedStatement adding = con.prepareStatement("INSERT INTO perms (guildName, guildId, roleName, roleId, permission) VALUES (?, ?, ?, ?, ?)");
            adding.setString(1, guild.getName());
            adding.setString(2, guild.getId());
            adding.setString(3, name);
            adding.setString(4, id);
            adding.setString(5, permission);
            adding.executeUpdate();
            adding.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePermission(Guild guild, Role role, String permission) {
        String id = role == null ? guild.getRolesByName("@everyone", false).get(0).getId() : role.getId();
        try {
            PreparedStatement removing = con.prepareStatement("DELETE FROM perms WHERE guildId= ? AND roleId= ? AND permission= ?");
            removing.setString(1, guild.getId());
            removing.setString(2, id);
            removing.setString(3, permission);
            removing.executeUpdate();
            removing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPermission(Guild guild, Role role, String permission) {
        String id = role == null ? guild.getRolesByName("@everyone", false).get(0).getId() : role.getId();
        try {
            PreparedStatement getting = con.prepareStatement("SELECT * FROM perms WHERE guildId= ? AND roleId= ? AND permission= ?");
            getting.setString(1, guild.getId());
            getting.setString(2, id);
            getting.setString(3, permission);
            ResultSet rs = getting.executeQuery();
            if (rs.next()) return true;
            getting.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearPermissions(Guild guild, Role role) {
        String id = role == null ? guild.getRolesByName("@everyone", false).get(0).getId() : role.getId();
        try {
            PreparedStatement clearing = con.prepareStatement("DELETE FROM perms WHERE guildId= ? AND roleId= ?");
            clearing.setString(1, guild.getId());
            clearing.setString(2, id);
            clearing.executeUpdate();
            clearing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPermissions(Guild guild, Role role) {
        List<String> toReturn = new ArrayList<>();
        String id = role == null ? guild.getRolesByName("@everyone", false).get(0).getId() : role.getId();
        try {
            PreparedStatement getPerms = con.prepareStatement("SELECT * FROM perms WHERE guildId= ? AND roleId= ?");
            getPerms.setString(1, guild.getId());
            getPerms.setString(2, id);
            ResultSet rs = getPerms.executeQuery();
            while (rs.next()) {
                toReturn.add(rs.getString("permission"));
            }
            getPerms.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean noOneHasPermission(Guild guild, String permission) {
        List<String> roleNames = new ArrayList<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM perms WHERE guildId= ? AND permission= ?");
            statement.setString(1, guild.getId());
            statement.setString(2, permission);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                roleNames.add(rs.getString("roleName"));
            }
            statement.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roleNames.size() == 0;
    }

    public void copyPermissions(Guild guild, Role role1, Role role2) {
        List<String> permsRole1 = getPermissions(guild, role1);
        List<String> permsRole2 = getPermissions(guild, role2);
        for (String s : permsRole1) {
            if (!permsRole2.contains(s)) {

                addPermission(guild, role2, s);
            }
        }
    }

    //Prefix stuff---------------------------------------------------------------
    public boolean setPrefix(String id, String arg) {
        try {
            if (getPrefix(id).equalsIgnoreCase(">")) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO prefixes (guildId, prefix) VALUES (?, ?)");
                setPrefix.setString(1, id);
                setPrefix.setString(2, arg);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE prefixes SET prefix= ? WHERE guildId= ?");
                updatePrefix.setString(1, arg);
                updatePrefix.setString(2, id);
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getPrefix(Guild guild) {
        return getPrefix(guild.getId());
    }

    public String getPrefix(String guildId) {
        try {
            PreparedStatement getPrefix = con.prepareStatement("SELECT * FROM prefixes WHERE guildId= ?");
            getPrefix.setString(1, guildId);
            ResultSet rs = getPrefix.executeQuery();
            if (rs.next()) return rs.getString("prefix");
            return ">";
        } catch (SQLException e) {
            e.printStackTrace();
            return ">";
        }
    }

    //Punishment stuff--------------------------------------------------------------
    public boolean setTempBan(User staff, User victim, Guild guild, String reason, long seconds) {
        if (seconds > 0) {
            Long moment = System.currentTimeMillis();
            Long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String name = victim.getName() + "#" + victim.getDiscriminator();
            String namep = staff.getName() + "#" + staff.getDiscriminator();
            try {
                EmbedBuilder banned = new EmbedBuilder();
                banned.setColor(Color.RED);
                banned.setDescription("```LDIF" + "\nBanned: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
                banned.setThumbnail(victim.getAvatarUrl());
                if (victim.getAvatarUrl() == null) banned.setThumbnail(victim.getDefaultAvatarUrl());
                banned.setAuthor("Banned by: " + namep, null, staff.getAvatarUrl());
                if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(banned.build()).queue();
                if (getChannelId(guild, ChannelType.LOG) != null) guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(banned.build()).queue();
                ResultSet rs = query("SELECT * FROM active_bans WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
                if (rs.next()) {//Player was banned so just update the times
                    PreparedStatement banupdate = con.prepareStatement("UPDATE active_bans SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                    banupdate.setString(1, victim.getId());
                    banupdate.setString(2, guild.getId());
                    banupdate.setString(3, reason);
                    banupdate.setLong(4, moment);
                    banupdate.setLong(5, until);
                    banupdate.setString(6, staff.getId());
                    banupdate.setString(7, victim.getId());
                    banupdate.setString(8, guild.getId());
                    banupdate.executeUpdate();
                } else {//nieuwe ban
                    PreparedStatement ban = con.prepareStatement("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                    ban.setString(1, guild.getId());
                    ban.setString(2, victim.getId());
                    ban.setString(3, staff.getId());
                    ban.setString(4, reason);
                    ban.setLong(5, moment);
                    ban.setLong(6, until);
                    ban.executeUpdate();
                }
                //add to history as active
                PreparedStatement banhistoire = con.prepareStatement("INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
                banhistoire.setString(1, guild.getId());
                banhistoire.setString(2, victim.getId());
                banhistoire.setString(3, staff.getId());
                banhistoire.setString(4, reason);
                banhistoire.setLong(5, moment);
                banhistoire.setLong(6, until);
                banhistoire.setBoolean(7, true);
                banhistoire.executeUpdate();
                guild.getController().ban(victim.getId(), 7, reason).queue();
                return true;
            } catch (SQLException | IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    public boolean setPermBan(User staff, User victim, Guild guild, String reason) {
        Long moment = System.currentTimeMillis();
        Long until = null;
        String name = victim.getName() + "#" + victim.getDiscriminator();
        String namep = staff.getName() + "#" + staff.getDiscriminator();
        try {
            EmbedBuilder banned = new EmbedBuilder();
            banned.setColor(Color.RED);
            banned.setDescription("```LDIF" + "\nBanned: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
            banned.setThumbnail(victim.getAvatarUrl());
            if (victim.getAvatarUrl() == null) banned.setThumbnail(victim.getDefaultAvatarUrl());
            banned.setAuthor("Permanently banned by: " + namep, null, staff.getAvatarUrl());
            if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(banned.build()).queue();
            if (getChannelId(guild, ChannelType.LOG) != null) guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(banned.build()).queue();
            ResultSet rs = query("SELECT * FROM active_bans WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
            if (rs.next()) {//Player was banned so just update the times
                PreparedStatement banupdate = con.prepareStatement("UPDATE active_bans SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                banupdate.setString(1, victim.getId());
                banupdate.setString(2, guild.getId());
                banupdate.setString(3, reason);
                banupdate.setLong(4, moment);
                banupdate.setLong(5, until);
                banupdate.setString(6, staff.getId());
                banupdate.setString(7, victim.getId());
                banupdate.setString(8, guild.getId());
                banupdate.executeUpdate();
            } else {//nieuwe ban
                PreparedStatement ban = con.prepareStatement("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                ban.setString(1, guild.getId());
                ban.setString(2, victim.getId());
                ban.setString(3, staff.getId());
                ban.setString(4, reason);
                ban.setLong(5, moment);
                ban.setLong(6, until);
                ban.executeUpdate();
            }
            //add to history as active
            PreparedStatement banhistoire = con.prepareStatement("INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
            banhistoire.setString(1, guild.getId());
            banhistoire.setString(2, victim.getId());
            banhistoire.setString(3, staff.getId());
            banhistoire.setString(4, reason);
            banhistoire.setLong(5, moment);
            banhistoire.setLong(6, until);
            banhistoire.setBoolean(7, true);
            banhistoire.executeUpdate();
            guild.getController().ban(victim.getId(), 7, reason).queue();
            return true;
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }

    public boolean unban(User toUnban, Guild guild, JDA jda) {
        if (toUnban != null) {
            try {
                ResultSet rs = query("SELECT * FROM active_bans WHERE guildId= '" + guild.getId() + "' AND victimId= '" + toUnban.getId() + "'");
                boolean t = false;
                while (rs.next()) {
                    User author = jda.retrieveUserById(rs.getString("authorId")).complete();
                    guild.getController().unban(toUnban.getId()).queue();
                    PreparedStatement unban = con.prepareStatement("UPDATE history_bans SET ACTIVE= ? WHERE victimId= ? AND guildId= ?");
                    unban.setBoolean(1, false);
                    unban.setString(2, toUnban.getId());
                    unban.setString(3, guild.getId());
                    unban.executeUpdate();
                    unban.close();
                    update("DELETE FROM active_bans WHERE guildId= '" + guild.getId() + "' AND victimId= '" + toUnban.getId() + "'");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor("Unbanned by: " + author.getName() + "#" + author.getDiscriminator(), null, author.getAvatarUrl());
                    eb.setDescription("```LDIF" + "\nUnbanned: " + toUnban.getName() + "#" + toUnban.getDiscriminator() + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(System.currentTimeMillis()) + "```");
                    eb.setThumbnail(toUnban.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.setColor(Color.green);
                    toUnban.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                    if (getChannelId(guild, ChannelType.LOG) != null) {
                        guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(eb.build()).queue();
                    }
                    t = true;
                }
                return t;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean addWarn(User staff, User victim, Guild guild, String reason) {
        try {
            reason = reason.replaceFirst(" ", "");
            PreparedStatement newWarn = con.prepareStatement("INSERT INTO warns(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);");
            newWarn.setString(1, guild.getId());
            newWarn.setString(2, victim.getId());
            newWarn.setString(3, staff.getId());
            newWarn.setString(4, reason);
            newWarn.setLong(5, System.currentTimeMillis());
            newWarn.executeUpdate();
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor("Warned by: " + staff.getName() + "#" + staff.getDiscriminator(), null, staff.getAvatarUrl());
            embedBuilder.setDescription("```LDIF\n" + "Warned: " + victim.getName() + "#" + victim.getDiscriminator() + "\n" + "Reason: " + reason + "\n" + "Guild: " + guild.getName() + "\n" + "Moment: " + millisToDate(System.currentTimeMillis()) + "\n" + "```");
            embedBuilder.setThumbnail(victim.getAvatarUrl());
            embedBuilder.setColor(Color.yellow);
            String logChannelId = getChannelId(guild, ChannelType.LOG);
            if (logChannelId != null) {
                if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build() + "\nTarget has private messages disabled.").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build()).queue();
            }
            if (!victim.isFake()) {
                victim.openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setTempMute(User staff, User victim, Guild guild, String reason, long seconds) {
        if (seconds > 0) {
            Long moment = System.currentTimeMillis();
            Long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String name = victim.getName() + "#" + victim.getDiscriminator();
            String namep = staff.getName() + "#" + staff.getDiscriminator();
            try {
                EmbedBuilder muted = new EmbedBuilder();
                muted.setColor(Color.BLUE);
                muted.setDescription("```LDIF" + "\nMuted: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
                muted.setThumbnail(victim.getAvatarUrl());
                if (victim.getAvatarUrl() == null) muted.setThumbnail(victim.getDefaultAvatarUrl());
                muted.setAuthor("Muted by: " + namep, null, staff.getAvatarUrl());
                if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(muted.build()).queue();
                if (getChannelId(guild, ChannelType.LOG) != null) guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(muted.build()).queue();
                ResultSet rs = query("SELECT * FROM active_mutes WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
                if (rs.next()) {//Player was banned so just update the times
                    PreparedStatement muteupdate = con.prepareStatement("UPDATE active_mutes SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                    muteupdate.setString(1, victim.getId());
                    muteupdate.setString(2, guild.getId());
                    muteupdate.setString(3, reason);
                    muteupdate.setLong(4, moment);
                    muteupdate.setLong(5, until);
                    muteupdate.setString(6, staff.getId());
                    muteupdate.setString(7, victim.getId());
                    muteupdate.setString(8, guild.getId());
                    muteupdate.executeUpdate();
                } else {//nieuwe mute
                    PreparedStatement mute = con.prepareStatement("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                    mute.setString(1, guild.getId());
                    mute.setString(2, victim.getId());
                    mute.setString(3, staff.getId());
                    mute.setString(4, reason);
                    mute.setLong(5, moment);
                    mute.setLong(6, until);
                    mute.executeUpdate();
                }
                //add to history as active
                PreparedStatement mutehistoire = con.prepareStatement("INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
                mutehistoire.setString(1, guild.getId());
                mutehistoire.setString(2, victim.getId());
                mutehistoire.setString(3, staff.getId());
                mutehistoire.setString(4, reason);
                mutehistoire.setLong(5, moment);
                mutehistoire.setLong(6, until);
                mutehistoire.setBoolean(7, true);
                mutehistoire.executeUpdate();
                return true;
            } catch (SQLException | IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    public boolean setPermMute(User staff, User victim, Guild guild, String reason) {
        Long moment = System.currentTimeMillis();
        String name = victim.getName() + "#" + victim.getDiscriminator();
        String namep = staff.getName() + "#" + staff.getDiscriminator();
        try {
            EmbedBuilder muted = new EmbedBuilder();
            muted.setColor(Color.BLUE);
            muted.setDescription("```LDIF" + "\nMuted: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(moment) + "```");
            muted.setThumbnail(victim.getAvatarUrl());
            if (victim.getAvatarUrl() == null) muted.setThumbnail(victim.getDefaultAvatarUrl());
            muted.setAuthor("Permanently muted by: " + namep, null, staff.getAvatarUrl());
            if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(muted.build()).queue();
            if (getChannelId(guild, ChannelType.LOG) != null) guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(muted.build()).queue();
            ResultSet rs = query("SELECT * FROM active_mutes WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
            if (rs.next()) {
                PreparedStatement muteupdate = con.prepareStatement("UPDATE active_mutes SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                muteupdate.setString(1, victim.getId());
                muteupdate.setString(2, guild.getId());
                muteupdate.setString(3, reason);
                muteupdate.setLong(4, moment);
                muteupdate.setBigDecimal(5, null);
                muteupdate.setString(6, staff.getId());
                muteupdate.setString(7, victim.getId());
                muteupdate.setString(8, guild.getId());
                muteupdate.executeUpdate();
            } else {//nieuwe mute
                PreparedStatement mute = con.prepareStatement("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                mute.setString(1, guild.getId());
                mute.setString(2, victim.getId());
                mute.setString(3, staff.getId());
                mute.setString(4, reason);
                mute.setLong(5, moment);
                mute.setBigDecimal(6, null);
                mute.executeUpdate();
            }
            //add to history as active
            PreparedStatement mutehistoire = con.prepareStatement("INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
            mutehistoire.setString(1, guild.getId());
            mutehistoire.setString(2, victim.getId());
            mutehistoire.setString(3, staff.getId());
            mutehistoire.setString(4, reason);
            mutehistoire.setLong(5, moment);
            mutehistoire.setBigDecimal(6, null);
            mutehistoire.setBoolean(7, true);
            mutehistoire.executeUpdate();
            return true;
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }

    public boolean unmute(User toUnmute, Guild guild, JDA jda) {
        if (toUnmute != null) {
            try {
                ResultSet rs = query("SELECT * FROM active_mutes WHERE guildId= '" + guild.getId() + "' AND victimId= '" + toUnmute.getId() + "'");
                boolean t = false;
                while (rs.next()) {
                    User author = jda.retrieveUserById(rs.getString("authorId")).complete();
                    PreparedStatement unmute = con.prepareStatement("UPDATE history_mutes SET ACTIVE= ? WHERE victimId= ? AND guildId= ?");
                    unmute.setBoolean(1, false);
                    unmute.setString(2, toUnmute.getId());
                    unmute.setString(3, guild.getId());
                    unmute.executeUpdate();
                    unmute.close();
                    update("DELETE FROM active_mutes WHERE guildId= '" + guild.getId() + "' AND victimId= '" + toUnmute.getId() + "'");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor("Unmuted by: " + author.getName() + "#" + author.getDiscriminator(), null, author.getAvatarUrl());
                    eb.setDescription("```LDIF" + "\nUnmuted: " + toUnmute.getName() + "#" + toUnmute.getDiscriminator() + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(System.currentTimeMillis()) + "```");
                    eb.setThumbnail(toUnmute.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.setColor(Color.green);
                    guild.getController().removeSingleRoleFromMember(guild.getMember(toUnmute), guild.getRoleById(getRoleId(guild, RoleType.MUTE))).queue();
                    toUnmute.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                    if (getChannelId(guild, ChannelType.LOG) != null) {
                        guild.getTextChannelById(getChannelId(guild, ChannelType.LOG)).sendMessage(eb.build()).queue();
                    }
                    t = true;
                }
                return t;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

        //Punishment getters
        public String[] getUserBans(User user, Guild guild, JDA jda) {
            try {
                PreparedStatement getbans = con.prepareStatement("SELECT * FROM history_bans WHERE victimId= ? AND guildId= ?");
                getbans.setString(1, user.getId());
                getbans.setString(2, guild.getId());
                ResultSet rs = getbans.executeQuery();
                int amount = 0;
                while (rs.next()) amount++;
                String[] bans = new String[amount];
                ResultSet rs2 = getbans.executeQuery();
                if (amount == 0) return new String[]{"no bans"};
                int progress = 0;
                while (rs2.next()) {
                    String endTime = rs2.getString("endTime").equalsIgnoreCase("NULL") ? "Infinity" : millisToDate(rs2.getLong("endTime"));
                    bans[progress] = String.valueOf("```ini\n" +
                            "[Banned by]: " + jda.retrieveUserById(rs2.getString("authorId")) +
                            "\n[Reason]: " + rs2.getString("reason") +
                            "\n[From]: " + millisToDate(rs2.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rs2.getString("active") +
                            "```");
                    progress++;
                }
                return bans;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new String[]{"no bans"};
        }

        public String[] getUserMutes(User user, Guild guild, JDA jda) {
            try {
                PreparedStatement getbans = con.prepareStatement("SELECT * FROM history_mutes WHERE victimId= ? AND guildId= ?");
                getbans.setString(1, user.getId());
                getbans.setString(2, guild.getId());
                ResultSet rs = getbans.executeQuery();
                int amount = 0;
                while (rs.next()) amount++;
                String[] bans = new String[amount];
                ResultSet rs2 = getbans.executeQuery();
                if (amount == 0) return new String[]{"no mutes"};
                int progress = 0;
                while (rs2.next()) {
                    String endTime = rs2.getString("endTime").equalsIgnoreCase("NULL") ? "Infinity" : millisToDate(rs2.getLong("endTime"));
                    bans[progress] = String.valueOf("```ini\n" +
                            "[Muted by]: " + jda.retrieveUserById(rs2.getString("authorId")) +
                            "\n[Reason]: " + rs2.getString("reason") +
                            "\n[From]: " + millisToDate(rs2.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rs2.getString("active") +
                            "```");
                    progress++;
                }
                return bans;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new String[]{"no mutes"};
        }

        public String[] getUserWarns(User user, Guild guild, JDA jda) {
            try {
                PreparedStatement getbans = con.prepareStatement("SELECT * FROM warns WHERE victimId= ? AND guildId= ?");
                getbans.setString(1, user.getId());
                getbans.setString(2, guild.getId());
                ResultSet rs = getbans.executeQuery();
                int amount = 0;
                while (rs.next()) amount++;
                String[] bans = new String[amount];
                ResultSet rs2 = getbans.executeQuery();
                if (amount == 0) return new String[]{"no warns"};
                int progress = 0;
                while (rs2.next()) {
                    bans[progress] = String.valueOf("```ini\n" +
                            "[Warned by]: " + jda.retrieveUserById(rs2.getString("authorId")) +
                            "\n[Reason]: " + rs2.getString("reason") +
                            "\n[Moment]: " + millisToDate(rs2.getLong("moment")) +
                            "```");
                    progress++;
                }
                return bans;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new String[]{"no warns"};
        }


    //log channel stuff----------------------------------------------------------

    public boolean setChannel(String guildId, String channelId, ChannelType type) {
        try {
            if (getChannelId(guildId, type) == null) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_channels (guildId, channelId) VALUES (?, ?)");
                setPrefix.setString(1, guildId);
                setPrefix.setString(2, channelId);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE " + type.toString().toLowerCase() + "_channels SET channelId= ? WHERE guildId= ?");
                updatePrefix.setString(1, channelId);
                updatePrefix.setString(2, guildId);
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getChannelId(Guild guild, ChannelType type) {
        return getChannelId(guild.getId(), type);
    }

    public String getChannelId(String guildId, ChannelType type) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?");
            getLogChannel.setString(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            String s = null;
            while (rs.next()) s = rs.getString("channelId");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //streamer stuff------------------------------------------------
    public boolean setStreamerMode(String guildId, boolean state) {
        try {
            if (!getStreamerMode(guildId)) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO streamer_modes (guildId, state) VALUES (?, ?)");
                setPrefix.setString(1, guildId);
                setPrefix.setBoolean(2, state);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE streamer_modes SET state= ? WHERE guildId= ?");
                updatePrefix.setBoolean(1, state);
                updatePrefix.setString(2, guildId);
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getStreamerMode(String guildId) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM streamer_modes WHERE guildId= ?");
            getLogChannel.setString(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            boolean s = false;
            while (rs.next()) s = rs.getBoolean("state");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setStreamUrl(Guild guild, String url) {
        try {
            if (getStreamUrl(guild) == null) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO stream_urls(guildId, url) VALUES (?, ?)");
                setPrefix.setString(1, guild.getId());
                setPrefix.setString(2, url);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE stream_urls SET url= ? WHERE guildId= ?");
                updatePrefix.setString(1, url);
                updatePrefix.setString(2, guild.getId());
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getStreamUrl(Guild guild) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM stream_urls WHERE guildId= ?");
            getLogChannel.setString(1, guild.getId());
            ResultSet rs = getLogChannel.executeQuery();
            String s = null;
            while (rs.next()) s = rs.getString("url");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Command stuff---------------------------------------------------
    public void addCommand(Command command) {
        try {
            PreparedStatement newCommand = con.prepareStatement("INSERT INTO commands(commandName, gebruik, description, extra, category, aliases) VALUES (?, ?, ?, ? , ?, ?)");
            newCommand.setString(1, command.getCommandName());
            newCommand.setString(2, command.getUsage());
            newCommand.setString(3, command.getDescription());
            newCommand.setString(4, command.getExtra());
            newCommand.setString(5, String.valueOf(command.getCategory()));
            newCommand.setString(6, Arrays.toString(command.getAliases()));
            newCommand.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Mute role stuff--------------------------------------------------
    public String getRoleId(Guild guild, RoleType type) {
        try {
            PreparedStatement getRoleId = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?");
            getRoleId.setString(1, guild.getId());
            ResultSet rs = getRoleId.executeQuery();
            String s = null;
            while (rs.next()) s = rs.getString("roleId");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean setRole(Guild guild, String roleId, RoleType type) {
        try {
            if (getRoleId(guild, type) == null) {
                PreparedStatement setRole = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_roles (guildId, roleId) VALUES (?, ?)");
                setRole.setString(1, guild.getId());
                setRole.setString(2, roleId);
                setRole.executeUpdate();
                setRole.close();
                return true;
            } else {
                PreparedStatement updateRole = con.prepareStatement("UPDATE " + type.toString().toLowerCase() + "_roles SET roleId= ? WHERE guildId= ?");
                updateRole.setString(1, roleId);
                updateRole.setString(2, guild.getId());
                updateRole.executeUpdate();
                updateRole.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
