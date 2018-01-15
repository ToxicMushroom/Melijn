package com.pixelatedsource.jda.db;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.ChannelType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
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

    public MySQL() {
        if (con == null) connect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + this.ip + ":3306/" + this.dbname + "?autoReconnect=true&useUnicode=true&useSSL=false", this.user, this.pass);
            Statement statement = con.createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
            System.out.println("[MySQL] has connected");
            update("CREATE TABLE IF NOT EXISTS log_channels(guildId varchar(64), channelId varchar(64))");
            update("CREATE TABLE IF NOT EXISTS perms(guildName varchar(64), guildId varchar(128), roleName varchar(64), roleId varchar(128), permission varchar(256));");
            update("CREATE TABLE IF NOT EXISTS music_channels(guildId varchar(64), channelId varchar(64))");
            update("CREATE TABLE IF NOT EXISTS prefixes(guildId varchar(128), prefix varchar(128));");
            update("CREATE TABLE IF NOT EXISTS active_bans(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint);");
            update("CREATE TABLE IF NOT EXISTS history_bans(guildId varchar(128), victimId varchar(128), authorId varchar(128), reason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            update("CREATE TABLE IF NOT EXISTS history_messages(guildId varchar(128), authorId varchar(128), messageId varchar(128), content varchar(3000), textChannelId varchar(128), sentTime bigint);");
            update("CREATE TABLE IF NOT EXISTS deleted_messages(guildId varchar(128), authorId varchar(128), messageId varchar(128), content varchar(3000), textChannelId varchar(128), sentTime bigint, delTime bigint);");
            update("CREATE TABLE IF NOT EXISTS unclaimed_messages(deletedMessageId varchar(64), logMessageId varchar(64));");
        } catch (SQLException e) {
            System.out.println((char) 27 + "[31m" + "did not connect");
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
            connect();
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

    public String getPrefix(String id) {
        try {
            PreparedStatement getPrefix = con.prepareStatement("SELECT * FROM prefixes WHERE guildId= ?");
            getPrefix.setString(1, id);
            ResultSet rs = getPrefix.executeQuery();
            if (rs.next()) return rs.getString("prefix");
            return ">";
        } catch (SQLException e) {
            e.printStackTrace();
            return "SQL error..";
        }
    }

    //Banning stuff--------------------------------------------------------------
    public boolean setTempBan(JDA jda, String guildId, String authorId, String victimId, String reason, long days) {
        if (days > 0) {
            Guild guild = jda.getGuildById(guildId);
            Long moment = System.currentTimeMillis();
            Long until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
            User victim = jda.retrieveUserById(victimId).complete();
            User staff = jda.retrieveUserById(authorId).complete();
            if (guild.getSelfMember().getRoles().size() == 0) return false;
            String name = victim.getName() + "#" + victim.getDiscriminator();
            String namep = staff.getName() + "#" + staff.getDiscriminator();
            try {
                EmbedBuilder banned = new EmbedBuilder();
                banned.setColor(Color.RED);
                banned.setDescription("```LDIF" + "\nBanned: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
                banned.setThumbnail(victim.getAvatarUrl());
                if (victim.getAvatarUrl() == null) banned.setThumbnail(victim.getDefaultAvatarUrl());
                banned.setAuthor("Banned by: " + namep, null, staff.getAvatarUrl());
                if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(banned.build()).queue();
                if (getLogChannelId(guildId) != null) jda.getGuildById(guildId).getTextChannelById(getLogChannelId(guildId)).sendMessage(banned.build()).queue();
                ResultSet rs = query("SELECT * FROM active_bans WHERE victimId= '" + victimId + "' AND guildId= '" + guildId + "'");
                if (rs.next()) {//Player was banned so just update the times
                    PreparedStatement banupdate = con.prepareStatement("UPDATE active_bans SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                    banupdate.setString(1, victimId);
                    banupdate.setString(2, guildId);
                    banupdate.setString(3, reason);
                    banupdate.setLong(4, moment);
                    banupdate.setLong(5, until);
                    banupdate.setString(6, authorId);
                    banupdate.setString(7, victimId);
                    banupdate.setString(8, guildId);
                    banupdate.executeUpdate();
                } else {//nieuwe ban
                    PreparedStatement ban = con.prepareStatement("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                    ban.setString(1, guildId);
                    ban.setString(2, victimId);
                    ban.setString(3, authorId);
                    ban.setString(4, reason);
                    ban.setLong(5, moment);
                    ban.setLong(6, until);
                    ban.executeUpdate();
                }
                //add to history as active
                PreparedStatement banhistoire = con.prepareStatement("INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
                banhistoire.setString(1, guildId);
                banhistoire.setString(2, victimId);
                banhistoire.setString(3, authorId);
                banhistoire.setString(4, reason);
                banhistoire.setLong(5, moment);
                banhistoire.setLong(6, until);
                banhistoire.setBoolean(7, true);
                banhistoire.executeUpdate();
                jda.getGuildById(guildId).getController().ban(victimId, 7, reason).queue();
                return true;
            } catch (SQLException | IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    public boolean unban(User toUnban, String guildid, JDA jda) {
        Guild guild = jda.getGuildById(guildid);
        if (toUnban != null) {
            try {
                ResultSet rs = query("SELECT * FROM active_bans WHERE guildId= '" + guildid + "' AND victimId= '" + toUnban.getId() + "'");
                boolean t = false;
                while (rs.next()) {
                    User author = jda.retrieveUserById(rs.getString("authorId")).complete();
                    guild.getController().unban(toUnban.getId()).queue();
                    PreparedStatement unban = con.prepareStatement("UPDATE history_bans SET ACTIVE= ? WHERE victimId= ? AND guildId= ?");
                    unban.setBoolean(1, false);
                    unban.setString(2, toUnban.getId());
                    unban.setString(3, guildid);
                    unban.executeUpdate();
                    unban.close();
                    update("DELETE FROM active_bans WHERE guildId= '" + guildid + "' AND victimId= '" + toUnban.getId() + "'");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor(author.getName() + "#" + author.getDiscriminator() + " has unbanned " + toUnban.getName() + "#" + toUnban.getDiscriminator(), null, author.getAvatarUrl());
                    eb.setThumbnail(toUnban.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    eb.setColor(Color.green);
                    toUnban.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                    if (getLogChannelId(guildid) != null) {
                        guild.getTextChannelById(getLogChannelId(guildid)).sendMessage(eb.build()).queue();
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

    public String getChannelId(String guildId,ChannelType type) {
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
}
