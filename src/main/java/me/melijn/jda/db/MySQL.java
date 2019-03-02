package me.melijn.jda.db;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.MessageType;
import me.melijn.jda.blub.*;
import me.melijn.jda.commands.management.CustomCommandCommand;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MySQL {

    private final Melijn melijn;
    private final HikariDataSource ds;
    private final Logger logger = LoggerFactory.getLogger(MySQL.class.getName());

    private long currentTime = 0;
    private int currentHour = 25; //Because 25 isn't a valid hour it will be changed on the first updateUsage call

    public MySQL(Melijn melijn, String ip, String user, String pass, String dbname) {
        this.melijn = melijn;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + ip + ":3306/" + dbname);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(20);
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("useLegacyDatetimeCode", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");
        //https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        config.addDataSourceProperty("allowMultiQueries", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "350");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");

        this.ds = new HikariDataSource(config);
        init();
    }

    private void init() {
        try {
            logger.info("[MySQL] has connected & Loading init");
            try (Connection con = ds.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    statement.executeQuery("SET NAMES 'utf8mb4'");
                }
            }

            //Commands
            executeUpdate("CREATE TABLE IF NOT EXISTS commands(commandName varchar(128), gebruik varchar(1024), description varchar(2048), extra varchar(2048), category varchar(128), aliases varchar(256), PRIMARY KEY (commandName));");
            executeUpdate("CREATE TABLE IF NOT EXISTS disabled_commands(guildId bigint, command int, UNIQUE KEY (guildId, command))");
            executeUpdate("CREATE TABLE IF NOT EXISTS custom_commands(guildId bigint, name varchar(128), description varchar(2048), aliases varchar(256), prefix boolean, attachment varchar(1024), message varchar(2048), UNIQUE KEY (guildId, name))");
            executeUpdate("CREATE TABLE IF NOT EXISTS command_usage(commandId int, usageCount bigint, time bigint, UNIQUE KEY(commandId, time))");

            //roles
            executeUpdate("CREATE TABLE IF NOT EXISTS self_roles(guildId bigint, roleId bigint, emote varchar(128), UNIQUE KEY (guildId, roleId, emote));");
            executeUpdate("CREATE TABLE IF NOT EXISTS mute_roles(guildId bigint, roleId bigint, PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS join_roles(guildId bigint, roleId bigint, PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS forced_roles(guildId bigint, userId bigint, roleId bigint, UNIQUE KEY (guildId, userId, roleId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS unverified_roles(guildId bigint, roleId bigint, PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS perms_roles(guildId bigint, roleId bigint, permission varchar(256), UNIQUE KEY (guildId, roleId, permission));");
            executeUpdate("CREATE TABLE IF NOT EXISTS perms_users(guildId bigint, userId bigint, permission varchar(256), UNIQUE KEY (guildId, userId, permission));");

            //channels
            executeUpdate("CREATE TABLE IF NOT EXISTS ban_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS mute_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS kick_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS warn_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS sdm_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS odm_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS pm_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS fm_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS em_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS reaction_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS attachment_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS music_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS welcome_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS music_log_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS self_role_channels(guildId bigint, channelId bigint, PRIMARY KEY (guildId))");


            //Other settings
            executeUpdate("CREATE TABLE IF NOT EXISTS embed_colors(guildId bigint, color bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS cooldowns(guildId bigint, commandId int, cooldown int, UNIQUE KEY (guildId, commandId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS stream_urls(guildId bigint, url varchar(2048), PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS prefixes(guildId bigint, prefix bigint, PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS private_prefixes(userId bigint, prefixes varchar(124), PRIMARY KEY (userId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_thresholds(guildId bigint, threshold tinyint, PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_types(guildId bigint, type varchar(64), PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_codes(guildId bigint, code varchar(2048), PRIMARY KEY (guildId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS unverified_users(guildId bigint, userId bigint, moment bigint, UNIQUE KEY (guildId, userId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS streamer_modes(guildId bigint, PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS filters(guildId bigint, mode varchar(16), content varchar(2048))");
            executeUpdate("CREATE TABLE IF NOT EXISTS join_messages(guildId bigint, content varchar(2048), PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS leave_messages(guildId bigint, content varchar(2048), PRIMARY KEY (guildId))");
            executeUpdate("CREATE TABLE IF NOT EXISTS nextvote_notifications(userId bigint, targetId bigint, UNIQUE KEY(userId, targetId));");

            //Backend saves
            executeUpdate("CREATE TABLE IF NOT EXISTS warns(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), moment bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS kicks(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), moment bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS active_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), startTime bigint, endTime bigint, UNIQUE KEY (guildId, victimId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), unbanReason varchar(2048), startTime bigint, endTime bigint, active boolean);");
            executeUpdate("CREATE TABLE IF NOT EXISTS active_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), startTime bigint, endTime bigint, UNIQUE KEY (guildId, victimId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2048), unmuteReason varchar(2048), startTime bigint, endTime bigint, active boolean);");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_messages(guildId bigint, authorId bigint, messageId bigint, content varchar(2048), textChannelId bigint, sentTime bigint, PRIMARY KEY (messageId));");
            executeUpdate("CREATE TABLE IF NOT EXISTS saved_queues(guildId bigint, position int, url varchar(1024), UNIQUE KEY (guildId, position))");
            executeUpdate("CREATE TABLE IF NOT EXISTS votes(userId bigint, votes bigint, streak bigint, lastTime bigint, PRIMARY KEY (userId));");

            //Cleanup commands
            executeUpdate("TRUNCATE TABLE commands");

            logger.info("[MySQL] init loaded");
        } catch (SQLException e) {
            logger.error("[MySQL] did not init -> ");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public int executeUpdate(final String query, final Object... objects) {
        try (final Connection connection = ds.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int current = 1;
            for (final Object object : objects) {
                preparedStatement.setObject(current++, object);
            }
            return preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            logger.error("Something went wrong while executing the query: " + query);
            e.printStackTrace();
        }
        return 0;
    }

    private void executeQuery(final String sql, final Consumer<ResultSet> consumer, final Object... objects) {
        melijn.getVariables().queryAmount++;
        try (final Connection connection = ds.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int current = 1;
            for (final Object object : objects) {
                preparedStatement.setObject(current++, object);
            }
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                consumer.accept(resultSet);
            }
        } catch (final SQLException e) {
            logger.error("Something went wrong while executing the query: " + sql);
            e.printStackTrace();
        }
    }

    public long getMessageAuthorId(long messageId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getAuthor = con.prepareStatement("SELECT * FROM history_messages WHERE messageId= ?")) {
            getAuthor.setLong(1, messageId);
            try (ResultSet rs = getAuthor.executeQuery()) {
                if (rs.next()) return rs.getLong("authorId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONObject getMessageObject(long messageId) {
        JSONObject jsonObject = new JSONObject();
        try (Connection con = ds.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM history_messages WHERE messageId=? LIMIT 1")) {
            preparedStatement.setLong(1, messageId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    jsonObject.put("authorId", rs.getLong("authorId"));
                    jsonObject.put("sentTime", rs.getLong("sentTime"));
                    jsonObject.put("content", rs.getString("content"));
                    jsonObject.put("guildId", rs.getLong("guildId"));
                    jsonObject.put("textChannelId", rs.getLong("textChannelId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public void createMessage(long messageId, String content, long authorId, long guildId, long textChannelId) {
        try (Connection con = ds.getConnection();
             PreparedStatement createMessage = con.prepareStatement("INSERT INTO history_messages(guildId, authorId, messageId, content, textChannelId, sentTime) VALUES (?, ?, ?, ?, ?, ?)")) {
            createMessage.setLong(1, guildId);
            createMessage.setLong(2, authorId);
            createMessage.setLong(3, messageId);
            createMessage.setString(4, content);
            createMessage.setLong(5, textChannelId);
            createMessage.setLong(6, System.currentTimeMillis());
            createMessage.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Permissions stuff---------------------------------------------------------
    public void addRolePermission(long guildId, long roleId, String permission) {
        try (Connection con = ds.getConnection();
             PreparedStatement adding = con.prepareStatement("INSERT INTO perms_roles(guildId, roleId, permission) VALUES (?, ?, ?)")) {
            setPermissionParams(guildId, roleId, permission, adding);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUserPermission(long guildId, long userId, String permission) {
        try (Connection con = ds.getConnection();
             PreparedStatement adding = con.prepareStatement("INSERT INTO perms_users(guildId, userId, permission) VALUES (?, ?, ?)")) {
            setPermissionParams(guildId, userId, permission, adding);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeRolePermission(long guildId, long roleId, String permission) {
        try (Connection con = ds.getConnection();
             PreparedStatement removing = con.prepareStatement("DELETE FROM perms_roles WHERE guildId= ? AND roleId= ? AND permission= ?")) {
            setPermissionParams(guildId, roleId, permission, removing);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setPermissionParams(long guildId, long roleId, String permission, PreparedStatement statement) throws SQLException {
        statement.setLong(1, guildId);
        statement.setLong(2, roleId);
        statement.setString(3, permission);
        statement.executeUpdate();
        statement.close();
    }

    public void removeUserPermission(long guildId, long userId, String permission) {
        try (Connection con = ds.getConnection();
             PreparedStatement removing = con.prepareStatement("DELETE FROM perms_users WHERE guildId= ? AND userId= ? AND permission= ?")) {
            setPermissionParams(guildId, userId, permission, removing);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPermission(Guild guild, long userId, String permission) {
        try (Connection con = ds.getConnection()) {
            List<Long> roles = guild.getMemberById(userId).getRoles().stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
            roles.add(guild.getIdLong()); //Everyone role
            StringBuilder sb = new StringBuilder("" +
                    "SELECT * FROM perms_users WHERE guildId= ? AND userId= ? AND (permission= ? OR permission= ?) UNION " +
                    "SELECT * FROM perms_roles WHERE guildId= ? AND roleId IN ("
            );
            for (int i = 0; i < roles.size(); i++) {
                sb.append("?").append(i == roles.size() - 1 ? "" : ",");
            }
            sb.append(") AND (permission= ? OR permission= ?) LIMIT 1");
            try (PreparedStatement getting = con.prepareStatement(sb.toString())) {
                getting.setLong(1, guild.getIdLong());
                getting.setLong(2, userId);
                getting.setString(3, permission);
                getting.setString(4, "*");
                getting.setLong(5, guild.getIdLong());
                int index = 6;
                for (long l : roles) {
                    getting.setLong(index++, l);
                }
                getting.setString(index++, permission);
                getting.setString(index, "*");
                try (ResultSet rs = getting.executeQuery()) {
                    return rs.next();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void clearRolePermissions(long guildId, long roleId) {
        try (Connection con = ds.getConnection();
             PreparedStatement clearing = con.prepareStatement("DELETE FROM perms_roles WHERE guildId= ? AND roleId= ?")) {
            clearing.setLong(1, guildId);
            clearing.setLong(2, roleId);
            clearing.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearUserPermissions(long guildId, long userId) {
        try (Connection con = ds.getConnection();
             PreparedStatement clearing = con.prepareStatement("DELETE FROM perms_users WHERE guildId= ? AND userId= ?")) {
            clearing.setLong(1, guildId);
            clearing.setLong(2, userId);
            clearing.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getRolePermissions(long guildId, long roleId) {
        Set<String> toReturn = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getPerms = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND roleId= ?")) {
            addResultsToSet(guildId, roleId, toReturn, getPerms);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public Set<String> getUserPermissions(long guildId, long userId) {
        Set<String> toReturn = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getPerms = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND userId= ?")) {
            addResultsToSet(guildId, userId, toReturn, getPerms);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    private void addResultsToSet(long guildId, long userId, Set<String> list, PreparedStatement statement) throws SQLException {
        statement.setLong(1, guildId);
        statement.setLong(2, userId);
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("permission"));
            }
            statement.close();
        }
    }

    public boolean noOneHasPermission(long guildId, String permission) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("" +
                     "SELECT * FROM perms_roles WHERE guildId= ? AND permission= ? UNION " +
                     "SELECT * FROM perms_users WHERE guildId= ? AND permission= ? LIMIT 1")
        ) {
            statement.setLong(1, guildId);
            statement.setString(2, permission);
            statement.setLong(3, guildId);
            statement.setString(4, permission);
            try (ResultSet rs = statement.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void copyRolePermissions(long guildId, long roleId1, long roleId2) {
        Set<String> permsRole1 = getRolePermissions(guildId, roleId1);
        Set<String> permsRole2 = getRolePermissions(guildId, roleId2);
        for (String permission : permsRole1) {
            if (!permsRole2.contains(permission)) {
                addRolePermission(guildId, roleId2, permission);
            }
        }
    }

    public void copyUserPermissions(long guildId, long userId1, long userId2) {
        Set<String> permsUser1 = getUserPermissions(guildId, userId1);
        Set<String> permsUser2 = getUserPermissions(guildId, userId2);
        for (String permission : permsUser1) {
            if (!permsUser2.contains(permission)) {
                addUserPermission(guildId, userId2, permission);
            }
        }
    }

    public void copyRoleUserPermissions(long guildId, long roleId, long userId) {
        Set<String> permsRole = getRolePermissions(guildId, roleId);
        Set<String> permsUser = getUserPermissions(guildId, userId);
        for (String permission : permsRole) {
            if (!permsUser.contains(permission)) {
                addUserPermission(guildId, userId, permission);
            }
        }
    }

    public void copyUserRolePermissions(long guildId, long userId, long roleId) {
        Set<String> permsUser = getUserPermissions(guildId, userId);
        Set<String> permsRole = getRolePermissions(guildId, roleId);
        for (String permission : permsUser) {
            if (!permsRole.contains(permission)) {
                addRolePermission(guildId, roleId, permission);
            }
        }
    }

    //Prefix stuff---------------------------------------------------------------
    public void setPrefix(long guildId, String prefix) {
        executeUpdate("INSERT INTO prefixes (guildId, prefix) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefix= ?",
                guildId, prefix, prefix);
        melijn.getVariables().prefixes.put(guildId, prefix);
    }

    public String getPrefix(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getPrefix = con.prepareStatement("SELECT * FROM prefixes WHERE guildId= ? LIMIT 1")) {
            getPrefix.setLong(1, guildId);
            try (ResultSet rs = getPrefix.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prefix");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Melijn.PREFIX;
    }

    //Punishment stuff--------------------------------------------------------------
    public boolean setTempBan(User author, User target, Guild guild, String reasonRaw, long seconds) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        if (seconds <= 0) return false;
        long moment = System.currentTimeMillis();
        long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        String namet = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();
        EmbedBuilder banned = new EmbedBuilder();
        banned.setColor(Color.RED);
        banned.setDescription("```LDIF" +
                "\nBanned: " + namet +
                "\nTargetID: " + target.getId() +
                "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") +
                "\nGuild: " + guild.getName() +
                "\nFrom: " + melijn.getMessageHelper().millisToDate(moment) +
                "\nUntil: " + melijn.getMessageHelper().millisToDate(until) + "```");
        banned.setThumbnail(target.getEffectiveAvatarUrl());
        banned.setAuthor("Banned by: " + name + " ".repeat(80).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot() && !target.isFake())
            target.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(banned.build()).queue(
                    (success) -> guild.getController().ban(target, 7, reason).queue(),
                    (failed) -> guild.getController().ban(target, 7, reason).queue()
            ), (failed) -> guild.getController().ban(target, 7, reason).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().banLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot() && !target.isFake())
                logChannel.sendMessage(banned.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(banned.build()).queue();
        }
        executeUpdate("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, author.getIdLong(), reason, moment, until,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, true);
        return true;
    }

    public boolean setPermBan(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        String nameTarget = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();
        EmbedBuilder banned = new EmbedBuilder();
        banned.setColor(Color.RED);
        banned.setDescription("```LDIF" +
                "\nBanned: " + nameTarget +
                "\nTargetID: " + target.getId() +
                "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") +
                "\nGuild: " + guild.getName() +
                "\nMoment: " + melijn.getMessageHelper().millisToDate(moment) + "```");
        banned.setThumbnail(target.getEffectiveAvatarUrl());
        banned.setAuthor("Banned by: " + name + " ".repeat(80).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot() && !target.isFake())
            target.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(banned.build()).queue(
                    (success) -> guild.getController().ban(target, 7, reason).queue(),
                    (failed) -> guild.getController().ban(target, 7, reason).queue()
            ), (failed) -> guild.getController().ban(target, 7, reason).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().banLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot() && !target.isFake())
                logChannel.sendMessage(banned.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(banned.build()).queue();
        }
        executeUpdate("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, author.getIdLong(), reason, moment, null,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, true);
        return true;
    }

    public boolean unban(User toUnban, Guild guild, User author, String reason) {
        if (toUnban == null) return false;
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM active_bans WHERE guildId= ? AND victimId= ?")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, toUnban.getIdLong());
            if (statement.executeUpdate() > 0) {
                executeUpdate("UPDATE history_bans SET active= ? AND unbanReason= ? WHERE victimId= ? AND guildId= ?",
                        false, reason, toUnban.getIdLong(), guild.getIdLong());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("Unbanned by: " + author.getName() + "#" + author.getDiscriminator() + " ".repeat(80).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
        eb.setDescription("```LDIF" +
                "\nUnbanned: " + toUnban.getName() + "#" + toUnban.getDiscriminator() +
                "\nTargetID: " + toUnban.getId() +
                "\nReason: " + reason +
                "\nGuild: " + guild.getName() +
                "\nMoment: " + melijn.getMessageHelper().millisToDate(System.currentTimeMillis()) + "```");
        eb.setThumbnail(toUnban.getEffectiveAvatarUrl());
        eb.setColor(Color.green);

        if (!toUnban.isBot() && !toUnban.isFake())
            toUnban.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().banLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (toUnban.isBot())
                logChannel.sendMessage(eb.build()).append("\nTarget is a bot").queue();
            else logChannel.sendMessage(eb.build()).queue();
        }

        guild.getController().unban(toUnban.getId()).queue(success -> {
        }, failed -> {
        });
        return true;
    }

    public boolean hardUnban(long targetId, long guildId, String reason) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM active_bans WHERE guildId= ? AND victimId= ?")) {
            statement.setLong(1, guildId);
            statement.setLong(2, targetId);
            if (statement.executeUpdate() > 0) {
                executeUpdate("UPDATE history_bans SET active= ? AND unbanReason= ? WHERE victimId= ? AND guildId= ?",
                        false, reason, targetId, guildId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean softUnban(long targetId, Guild guild, String reason) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM active_bans WHERE guildId= ? AND victimId= ?")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, targetId);
            if (statement.executeUpdate() > 0) {
                executeUpdate("UPDATE history_bans SET active= ? AND unbanReason= ? WHERE victimId= ? AND guildId= ?",
                        false, reason, targetId, guild.getIdLong());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            guild.getController().unban(Long.toString(targetId)).queue();
        }
        return true;
    }

    public boolean addWarn(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        executeUpdate("INSERT INTO warns(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor("Warned by: " + author.getName() + "#" + author.getDiscriminator() + " ".repeat(80).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
        embedBuilder.setDescription("```LDIF" +
                "\nWarned: " + target.getName() + "#" + target.getDiscriminator() +
                "\nTargetID: " + target.getId() +
                "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") +
                "\nGuild: " + guild.getName() +
                "\nMoment: " + melijn.getMessageHelper().millisToDate(moment) + "```");
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        embedBuilder.setColor(Color.yellow);

        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().warnLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot()) logChannel.sendMessage(embedBuilder.build()).append("Target is a bot.").queue();
            else logChannel.sendMessage(embedBuilder.build()).queue();
        }
        if (!target.isBot()) target.openPrivateChannel().queue((m) -> m.sendMessage(embedBuilder.build()).queue());
        return true;
    }

    public boolean setTempMute(User author, User target, Guild guild, String reasonRaw, long seconds) {
        if (seconds <= 0) return false;

        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        String nameTarget = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();

        EmbedBuilder muted = new EmbedBuilder();
        muted.setColor(Color.BLUE);
        muted.setDescription("```LDIF\nMuted: " + nameTarget + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + melijn.getMessageHelper().millisToDate(moment) + "\nUntil: " + melijn.getMessageHelper().millisToDate(until) + "```");
        muted.setThumbnail(target.getEffectiveAvatarUrl());
        muted.setAuthor("Muted by: " + name + " ".repeat(45).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot()) target.openPrivateChannel().queue(pc -> pc.sendMessage(muted.build()).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().muteLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot()) logChannel.sendMessage(muted.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(muted.build()).queue();
        }
        executeUpdate("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until,
                author.getIdLong(), reason, moment, until,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, true);
        return true;

    }

    public boolean setPermMute(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        String nameTarget = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();
        EmbedBuilder muted = new EmbedBuilder();
        muted.setColor(Color.BLUE);
        muted.setDescription("```LDIF\nMuted: " + nameTarget + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + melijn.getMessageHelper().millisToDate(moment) + "```");
        muted.setThumbnail(target.getEffectiveAvatarUrl());
        muted.setAuthor("Muted by: " + name + " ".repeat(45).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot()) target.openPrivateChannel().queue(pc -> pc.sendMessage(muted.build()).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().muteLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot()) logChannel.sendMessage(muted.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(muted.build()).queue();
        }
        executeUpdate("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null,
                author.getIdLong(), reason, moment, null,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, true);
        return true;
    }

    public boolean unmute(Member member, User author, String reason) {
        User toUnmute = member.getUser();
        Guild guild = member.getGuild();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM active_mutes WHERE guildId= ? AND victimId= ?")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, toUnmute.getIdLong());
            if (statement.executeUpdate() > 0) {
                executeUpdate("UPDATE history_mutes SET active= ? AND unmuteReason= ? WHERE victimId= ? AND guildId= ?",
                        false, reason, toUnmute.getIdLong(), guild.getIdLong());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("Unmuted by: " + author.getName() + "#" + author.getDiscriminator() + " ".repeat(45).substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
        eb.setDescription("```LDIF" + "\nUnmuted: " + toUnmute.getName() + "#" + toUnmute.getDiscriminator() + "\nTargetID: " + toUnmute.getId() + "\nReason: " + reason + "\nGuild: " + guild.getName() + "\nMoment: " + melijn.getMessageHelper().millisToDate(System.currentTimeMillis()) + "```");
        eb.setThumbnail(toUnmute.getEffectiveAvatarUrl());
        eb.setColor(Color.green);

        if (!toUnmute.isBot() && !toUnmute.isFake())
            toUnmute.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().muteLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (toUnmute.isBot()) logChannel.sendMessage(eb.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(eb.build()).queue();
        }

        Member toUnmuteMember = guild.getMember(toUnmute);
        Role role = guild.getRoleById(melijn.getVariables().muteRoleCache.getUnchecked(guild.getIdLong()));
        if (toUnmuteMember != null && role != null && guild.getSelfMember().canInteract(toUnmuteMember) && guild.getSelfMember().canInteract(role))
            guild.getController().removeSingleRoleFromMember(toUnmuteMember, role).queue();

        return true;
    }

    public boolean hardUnmute(long guildId, long targetId, String reason) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM active_mutes WHERE guildId= ? AND victimId= ?")) {
            statement.setLong(1, guildId);
            statement.setLong(2, targetId);
            if (statement.executeUpdate() > 0) {
                executeUpdate("UPDATE history_mutes SET active= ? AND unmuteReason= ? WHERE victimId= ? AND guildId= ?",
                        false, reason, targetId, guildId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean addKick(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        executeUpdate("INSERT INTO kicks(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor("Kicked by: " + author.getName() + "#" + author.getDiscriminator() + " ".repeat(45).substring(0, 45 - author.getName().length()) + "\u200B",
                null,
                author.getEffectiveAvatarUrl());
        embedBuilder.setDescription("```LDIF" +
                "\nKicked: " + target.getName() + "#" + target.getDiscriminator() +
                "\nTargetID: " + target.getId() +
                "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") +
                "\nGuild: " + guild.getName() +
                "\nMoment: " + melijn.getMessageHelper().millisToDate(moment) + "```");
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        embedBuilder.setColor(Color.ORANGE);

        TextChannel logChannel = guild.getTextChannelById(melijn.getVariables().kickLogChannelCache.getUnchecked(guild.getIdLong()));
        if (logChannel != null && guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_WRITE)) {
            if (target.isBot()) logChannel.sendMessage(embedBuilder.build()).append("Target is a bot").queue();
            else logChannel.sendMessage(embedBuilder.build()).queue();
        }
        if (!target.isBot()) target.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(embedBuilder.build()).queue(
                    (success) -> guild.getController().kick(guild.getMember(target)).queue(),
                    (failed) -> guild.getController().kick(guild.getMember(target)).queue());
        });
        return true;
    }

    //Punishment getters
    public void getUserBans(long guildId, long userId, JDA jda, Consumer<String[]> bans) {
        Set<JSONObject> set = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getBans = con.prepareStatement("SELECT * FROM history_bans WHERE victimId= ? AND guildId= ?")) {
            getBans.setLong(1, userId);
            getBans.setLong(2, guildId);

            try (ResultSet rs = getBans.executeQuery()) {
                while (rs.next()) {
                    set.add(new JSONObject()
                            .put("authorId", rs.getLong("authorId"))
                            .put("reason", rs.getString("reason"))
                            .put("unbanReason", rs.getString("unbanReason") == null ? "" : rs.getString("unbanReason"))
                            .put("startTime", rs.getLong("startTime"))
                            .put("endTime", rs.getString("endTime") == null ? "" : rs.getString("endTime"))
                            .put("active", rs.getBoolean("active"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bans.accept(new String[]{"SQL Error :/ Contact support"});
        }
        if (set.size() == 0) {
            bans.accept(new String[]{"No bans"});
            return;
        }
        List<String> toRet = new ArrayList<>();
        AtomicInteger progress = new AtomicInteger();
        for (JSONObject rowObj : set) {
            String endTime = rowObj.getString("endTime").isEmpty() ? "Infinity" : melijn.getMessageHelper().millisToDate(Long.valueOf(rowObj.getString("endTime")));
            jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                if (rowObj.getBoolean("active"))
                    toRet.add("```ini" +
                            "\n[Banned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```");
                else {
                    String toAdd = "";
                    toAdd += "```ini" +
                            "\n[Banned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason");
                    if (rowObj.get("unbanReason") != null && !rowObj.getBoolean("active"))
                        toAdd += "\n[UnbanReason]: " + (rowObj.get("unbanReason") == null ? "N/A" : rowObj.getString("unbanReason"));
                    toAdd += "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```";
                    toRet.add(toAdd);
                }
                if (progress.incrementAndGet() == set.size()) {
                    bans.accept(toRet.toArray(new String[0]));
                }
            }, (failed) -> {
                toRet.add("```ini" +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                        "\n[Until]: " + endTime +
                        "\n[active]: " + rowObj.getBoolean("active") + "```");
                if (progress.incrementAndGet() == set.size()) {
                    bans.accept(toRet.toArray(new String[0]));
                }
            });
        }
    }

    public void getUserMutes(long guildId, long userId, JDA jda, Consumer<String[]> mutes) {
        Set<JSONObject> set = new HashSet<>();
        try (Connection connection = ds.getConnection();
             PreparedStatement getMutes = connection.prepareStatement("SELECT * FROM history_mutes WHERE victimId= ? AND guildId= ?")) {
            getMutes.setLong(1, userId);
            getMutes.setLong(2, guildId);

            try (ResultSet rs = getMutes.executeQuery()) {
                while (rs.next()) {
                    set.add(new JSONObject()
                            .put("authorId", rs.getLong("authorId"))
                            .put("reason", rs.getString("reason"))
                            .put("unmuteReason", rs.getString("unmuteReason") == null ? "" : rs.getString("unmuteReason"))
                            .put("startTime", rs.getLong("startTime"))
                            .put("endTime", rs.getString("endTime") == null ? "" : rs.getString("endTime"))
                            .put("active", rs.getBoolean("active"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mutes.accept(new String[]{"SQL Error :/ Contact support"});
        }
        if (set.size() == 0) {
            mutes.accept(new String[]{"No mutes"});
        }
        List<String> toRet = new ArrayList<>();
        AtomicInteger progress = new AtomicInteger();
        for (JSONObject rowObj : set) {
            String endTime = rowObj.getString("endTime").isEmpty() ? "Infinity" : melijn.getMessageHelper().millisToDate(Long.valueOf(rowObj.getString("endTime")));
            jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                if (rowObj.getBoolean("active"))
                    toRet.add("```ini" +
                            "\n[Muted by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```");
                else {
                    String toAdd = "";
                    toAdd += "```ini" +
                            "\n[Muted by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason");
                    if (!rowObj.get("unmuteReason").equals("") && !rowObj.getBoolean("active"))
                        toAdd += "\n[UnmuteReason]: " + (rowObj.get("unmuteReason") == null ? "N/A" : rowObj.getString("unmuteReason"));
                    toAdd += "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```";
                    toRet.add(toAdd);
                }
                if (progress.incrementAndGet() == set.size()) {
                    mutes.accept(toRet.toArray(new String[0]));
                }
            }, (failed) -> {
                toRet.add("```ini" +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[From]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("startTime")) +
                        "\n[Until]: " + endTime +
                        "\n[active]: " + rowObj.getBoolean("active") + "```");
                if (progress.incrementAndGet() == set.size()) {
                    mutes.accept(toRet.toArray(new String[0]));
                }
            });
        }
    }

    public boolean isUserMuted(long guildId, long userId) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        executeQuery("SELECT * FROM active_mutes WHERE victimId= ? AND guildId= ? LIMIT 1", rs -> {
            try {
                atomicBoolean.set(rs.next());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, userId, guildId);
        return atomicBoolean.get();
    }

    public void getUserWarns(long guildId, long userId, JDA jda, Consumer<String[]> warns) {
        Set<JSONObject> set = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getWarns = con.prepareStatement("SELECT * FROM warns WHERE victimId= ? AND guildId= ?")) {
            getWarns.setLong(1, userId);
            getWarns.setLong(2, guildId);

            try (ResultSet rs = getWarns.executeQuery()) {
                while (rs.next()) {
                    set.add(new JSONObject()
                            .put("authorId", rs.getLong("authorId"))
                            .put("reason", rs.getString("reason"))
                            .put("moment", rs.getLong("moment"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            warns.accept(new String[]{"SQL Error :/ Contact support"});
        }
        if (set.size() == 0) {
            warns.accept(new String[]{"No warns"});
        }
        List<String> toRet = new ArrayList<>();
        AtomicInteger progress = new AtomicInteger();
        for (JSONObject rowObj : set) {
            jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                toRet.add("```ini" +
                        "\n[Warned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[Moment]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("moment")) + "```");
                if (progress.incrementAndGet() == set.size()) {
                    warns.accept(toRet.toArray(new String[0]));
                }
            }, (failed) -> {
                toRet.add("```ini" +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[Moment]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("moment")) + "```");
                if (progress.incrementAndGet() == set.size()) {
                    warns.accept(toRet.toArray(new String[0]));
                }
            });
        }
    }


    public void getUserKicks(long guildId, long userId, JDA jda, Consumer<String[]> kicks) {
        Set<JSONObject> set = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getKicks = con.prepareStatement("SELECT * FROM kicks WHERE victimId= ? AND guildId= ?")) {
            getKicks.setLong(1, userId);
            getKicks.setLong(2, guildId);


            try (ResultSet rs = getKicks.executeQuery()) {
                while (rs.next()) {
                    set.add(new JSONObject()
                            .put("authorId", rs.getLong("authorId"))
                            .put("reason", rs.getString("reason"))
                            .put("moment", rs.getLong("moment"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            kicks.accept(new String[]{"SQL Error :/ Contact support"});
        }
        if (set.size() == 0) {
            kicks.accept(new String[]{"No kicks"});
        }
        List<String> toRet = new ArrayList<>();
        AtomicInteger progress = new AtomicInteger();
        for (JSONObject rowObj : set) {
            jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                toRet.add("```ini" +
                        "\n[Kicked by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[Moment]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("moment")) + "```");
                if (progress.incrementAndGet() == set.size()) {
                    kicks.accept(toRet.toArray(new String[0]));
                }
            }, (failed) -> {
                toRet.add("```ini" +
                        "\n[Reason]: " + rowObj.getString("reason") +
                        "\n[Moment]: " + melijn.getMessageHelper().millisToDate(rowObj.getLong("moment")) + "```");
                if (progress.incrementAndGet() == set.size()) {
                    kicks.accept(toRet.toArray(new String[0]));
                }
            });
        }
    }


    //log channel stuff----------------------------------------------------------340081887265685504-467770083326951446

    public boolean setChannel(long guildId, long channelId, ChannelType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_channels (guildId, channelId) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE channelId= ?",
                guildId, channelId, channelId);
        return true;
    }

    public long getChannelId(long guildId, ChannelType type) {
        try (Connection con = ds.getConnection();
             PreparedStatement getChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ? LIMIT 1")) {
            getChannel.setLong(1, guildId);
            try (ResultSet rs = getChannel.executeQuery()) {
                if (rs.next()) return rs.getLong("channelId");
                else return -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void removeChannel(long guildId, ChannelType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?",
                guildId);
    }

    //streamer stuff------------------------------------------------
    public void setStreamerMode(long guildId, boolean state) {
        if (state) {
            if (!melijn.getVariables().streamerModeCache.getUnchecked(guildId)) {
                executeUpdate("INSERT INTO streamer_modes (guildId) VALUES (?)", guildId);
            }
        } else {
            executeUpdate("DELETE FROM streamer_modes WHERE guildId= ?", guildId);
        }
    }

    public boolean getStreamerMode(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM streamer_modes WHERE guildId= ? LIMIT 1")) {
            getLogChannel.setLong(1, guildId);
            try (ResultSet rs = getLogChannel.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setStreamUrl(long guildId, String url) {
        executeUpdate("INSERT INTO stream_urls(guildId, url) VALUES (?, ?) ON DUPLICATE KEY UPDATE url= ?",
                guildId, url, url);
        return true;
    }

    public String getStreamUrl(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getStreamUrl = con.prepareStatement("SELECT * FROM stream_urls WHERE guildId= ? LIMIT 1")) {
            getStreamUrl.setLong(1, guildId);
            try (ResultSet rs = getStreamUrl.executeQuery()) {
                return rs.next() ? rs.getString("url") : "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Command stuff---------------------------------------------------
    public void addCommand(Command command) {
        executeUpdate("INSERT INTO commands(commandName, gebruik, description, extra, category, aliases) VALUES (?, ?, ?, ? , ?, ?)",
                command.getCommandName(),
                command.getUsage(),
                command.getDescription(),
                command.getExtra(),
                String.valueOf(command.getCategory()),
                Arrays.toString(command.getAliases()));
    }

    //Mute role stuff--------------------------------------------------
    public long getRoleId(long guildId, RoleType type) {
        try (Connection con = ds.getConnection();
             PreparedStatement getRoleId = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ? LIMIT 1")) {
            getRoleId.setLong(1, guildId);
            try (ResultSet rs = getRoleId.executeQuery()) {
                return rs.next() ? rs.getLong("roleId") : -1L;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean setRole(long guildId, long roleId, RoleType type) {
        if (roleId == -1L) {
            removeRole(guildId, type);
        } else {
            executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_roles (guildId, roleId) VALUES (?, ?) ON DUPLICATE KEY UPDATE roleId= ?", guildId, roleId, roleId);
        }
        return true;
    }

    public void removeRole(long guildId, RoleType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?", guildId);
    }

    //Filter stuff-----------------------------------------
    public void addFilter(long guildId, String mode, String content) {
        executeUpdate("INSERT IGNORE INTO filters (guildId, mode, content) VALUES (?, ?, ?)", guildId, mode, content);
    }

    public void removeFilter(long guildId, String mode, String content) {
        executeUpdate("DELETE FROM filters WHERE guildId= ? AND mode= ? AND content= ?", guildId, mode, content);
    }

    public List<String> getFilters(long guildId, String mode) {
        List<String> filters = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement addFilter = con.prepareStatement("SELECT * FROM filters WHERE guildId= ? AND mode= ?")) {
            addFilter.setLong(1, guildId);
            addFilter.setString(2, mode);
            try (ResultSet rs = addFilter.executeQuery()) {
                while (rs.next()) {
                    filters.add(rs.getString("content"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return filters;
    }

    //Message stuff ---------------------------------------------------------
    public String getMessage(long guildId, MessageType type) {
        try (Connection con = ds.getConnection();
             PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_messages WHERE guildId= ? LIMIT 1")) {
            getLogChannel.setLong(1, guildId);
            try (ResultSet rs = getLogChannel.executeQuery()) {
                return rs.next() ? rs.getString("content") : "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean setMessage(long guildId, String content, MessageType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_messages (guildId, content) VALUES (?, ?) ON DUPLICATE KEY UPDATE content= ?",
                guildId, content, content);
        return true;
    }

    public void removeMessage(long guildId, MessageType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_messages WHERE guildId= ?", guildId);
    }

    public JSONObject getVotesObject(long userId) {
        int bonus = userId == 231459866630291459L || userId == 258939128870207488L ? 1 : 0;
        JSONObject toReturn = new JSONObject().put("streak", bonus);
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM votes WHERE userId= ? LIMIT 1")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? toReturn.put("votes", rs.getLong("votes"))
                        .put("streak", rs.getLong("streak") + bonus)
                        .put("lastTime", rs.getLong("lastTime")) :
                        toReturn;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return toReturn;
        }
    }

    public Map<Long, Set<Long>> getNotificationsMap(NotificationType nextvote) {
        //userId -> mensen waarvan notificatie moet krijgen -> aan of uit
        Map<Long, Set<Long>> mapje = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM " + nextvote.toString().toLowerCase() + "_notifications");
             ResultSet rs = statement.executeQuery()) {
            Set<Long> row = new HashSet<>();
            while (rs.next()) {
                row.add(rs.getLong("userId"));
            }
            for (long s : row) {
                rs.beforeFirst();
                Set<Long> lijst = new HashSet<>();
                while (rs.next()) {
                    if (rs.getLong("userId") == s) {
                        lijst.add(rs.getLong("targetId"));
                    }
                }
                mapje.put(s, lijst);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public void putNotification(long userId, long targetId, NotificationType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_notifications (userId, targetId) VALUES (?, ?)",
                userId, targetId);
    }

    public void removeNotification(long userId, long targetId, NotificationType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_notifications WHERE userId= ? AND targetId= ?",
                userId, targetId);
    }

    public Set<Long> getVoteList() {
        Set<Long> list = new HashSet<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getVoteMap = con.prepareStatement("SELECT * FROM votes WHERE lastTime BETWEEN ? AND ?")) {
            long yesterdayAndMinute = System.currentTimeMillis() - 43_260_000L;
            long yesterday = System.currentTimeMillis() - 43_200_000L;
            getVoteMap.setLong(1, yesterdayAndMinute);
            getVoteMap.setLong(2, yesterday);
            try (ResultSet rs = getVoteMap.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong("userId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateVoteStreak() {
        executeUpdate("UPDATE votes SET streak=? WHERE lastTime<?", 0, System.currentTimeMillis() - 172_800_000);
    }

    public void addUnverifiedUser(long guildId, long userId, long time) {
        executeUpdate("INSERT INTO unverified_users (guildId, userId, moment) VALUES (?, ?, ?)",
                guildId, userId, time);
    }

    public void removeUnverifiedUser(long guildId, long userId) {
        executeUpdate("DELETE FROM unverified_users WHERE guildId= ? AND userId= ?", guildId, userId);
    }

    public void setVerificationCode(long guildId, String code) {
        executeUpdate("INSERT INTO verification_codes (guildId, code) VALUES (?, ?) ON DUPLICATE KEY UPDATE code= ?",
                guildId, code, code);
    }

    public void removeVerificationCode(long guildId) {
        executeUpdate("DELETE FROM verification_codes WHERE guildId= ?", guildId);
    }

    public void setVerificationThreshold(long guildId, int threshold) {
        executeUpdate("INSERT INTO verification_thresholds (guildId, threshold) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE threshold= ?",
                guildId, threshold, threshold);
    }

    public void removeVerificationThreshold(long guildId) {
        executeUpdate("DELETE FROM verification_thresholds WHERE guildId= ?", guildId);
    }

    public Map<Long, List<Integer>> getDisabledCommandsMap() {
        Map<Long, List<Integer>> toReturn = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM disabled_commands");
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                if (toReturn.containsKey(rs.getLong("guildId"))) {
                    List<Integer> buffertje = toReturn.get(rs.getLong("guildId"));
                    buffertje.add(rs.getInt("command"));
                    toReturn.put(rs.getLong("guildId"), buffertje);
                } else {
                    List<Integer> temp = new ArrayList<>();
                    temp.add(rs.getInt("command"));
                    toReturn.put(rs.getLong("guildId"), temp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public void removeDisabledCommands(long guildId, List<Integer> disabledBuffer) {
        List<Integer> toRemove = new ArrayList<>();
        if (melijn.getVariables().disabledGuildCommands.containsKey(guildId))
            toRemove.addAll(melijn.getVariables().disabledGuildCommands.get(guildId));
        toRemove.removeAll(disabledBuffer);
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM disabled_commands WHERE guildId= ? AND command= ?")) {
            for (int i : toRemove) {
                statement.setLong(1, guildId);
                statement.setInt(2, i);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDisabledCommands(long guildId, List<Integer> buffer) {
        List<Integer> toAdd = new ArrayList<>(buffer);
        if (melijn.getVariables().disabledGuildCommands.containsKey(guildId))
            toAdd.removeAll(melijn.getVariables().disabledGuildCommands.get(guildId));
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement("INSERT INTO disabled_commands (guildId, command) VALUES (?, ?)")) {
                for (int i : toAdd) {
                    statement.setLong(1, guildId);
                    statement.setInt(2, i);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Long, Long> getUnverifiedMembers(long guildId) {
        Map<Long, Long> members = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getUnverifiedMembers = con.prepareStatement("SELECT * FROM unverified_users WHERE guildId= ?")) {
            getUnverifiedMembers.setLong(1, guildId);
            try (ResultSet rs = getUnverifiedMembers.executeQuery()) {
                while (rs.next()) {
                    members.put(rs.getLong("userId"), rs.getLong("moment"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public int getGuildVerificationThreshold(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getVerificationThreshold = con.prepareStatement("SELECT * FROM verification_thresholds WHERE guildId= ? LIMIT 1")) {
            getVerificationThreshold.setLong(1, guildId);
            try (ResultSet rs = getVerificationThreshold.executeQuery()) {
                return rs.next() ? rs.getInt("threshold") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getGuildVerificationCode(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement getVerificationThreshold = con.prepareStatement("SELECT * FROM verification_codes WHERE guildId= ? LIMIT 1")) {
            getVerificationThreshold.setLong(1, guildId);
            try (ResultSet rs = getVerificationThreshold.executeQuery()) {
                if (rs.next()) return rs.getString("code");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public List<Long> getNotifications(long userId, NotificationType type) {
        List<Long> lijstje = new ArrayList<>();
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_notifications WHERE userId= ?")) {
                statement.setLong(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        lijstje.add(rs.getLong("targetId"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lijstje;
    }

    public void doUnbans(JDA jda) {
        Map<Long, Long> victimGuilds = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM active_bans WHERE endTime < ?")) {
            statement.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    victimGuilds.put(rs.getLong("victimId"), rs.getLong("guildId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        victimGuilds.forEach((victimId, guildId) -> {
            Guild guild = jda.asBot().getShardManager().getGuildById(guildId);
            if (guild == null) {
                hardUnban(victimId, guildId, "Ban expired");
                return;
            }
            softUnban(victimId, guild, "Ban expired");
        });
    }

    public void doUnmutes(JDA jda) {
        Map<Long, Long> victimGuilds = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM active_mutes WHERE endTime < ?")) {
            statement.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    victimGuilds.put(rs.getLong("victimId"), rs.getLong("guildId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        victimGuilds.forEach((victimId, guildId) -> {
            Guild guild = jda.asBot().getShardManager().getGuildById(guildId);
            if (guild == null) {
                hardUnmute(guildId, victimId, "Mute expired");
                return;
            }
            Member member = guild.getMemberById(victimId);
            if (member == null) {
                hardUnmute(guildId, victimId, "Mute expired");
                return;
            }
            unmute(member, jda.getSelfUser(), "Mute expired");

        });
    }

    public void updateUsage(int commandId, long currentTimeMillis) {
        if (currentHour != LocalTime.now().getHour()) {
            currentHour = LocalTime.now().getHour();
            currentTime = currentTimeMillis;
        }
        executeUpdate("INSERT INTO command_usage (commandId, time, usageCount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE usageCount= usageCount + 1 ",
                commandId, currentTime, 1);
    }

    public LinkedHashMap<Integer, Long> getTopUsage(long[] period, int limit) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        Map<Integer, Long> commandUsages = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getUsageWithinPeriod = con.prepareStatement("SELECT * FROM command_usage WHERE time < ? AND time > ?")) {
            getUsageWithinPeriod.setLong(1, biggest);
            getUsageWithinPeriod.setLong(2, smallest);
            try (ResultSet rs = getUsageWithinPeriod.executeQuery()) {
                while (rs.next()) {
                    commandUsages.put(rs.getInt("commandId"), (commandUsages.containsKey(rs.getInt("commandId")) ? commandUsages.get(rs.getInt("commandId")) : 0) + rs.getLong("usageCount"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sortMetricsMap(commandUsages, limit);
    }

    public long getUsage(long[] period, int commandId) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        long usage = 0;
        try (Connection con = ds.getConnection();
             PreparedStatement getUsageWithinPeriod = con.prepareStatement("SELECT * FROM command_usage WHERE commandId = ? AND time < ? AND time > ?")) {
            getUsageWithinPeriod.setInt(1, commandId);
            getUsageWithinPeriod.setLong(2, biggest);
            getUsageWithinPeriod.setLong(3, smallest);
            try (ResultSet rs = getUsageWithinPeriod.executeQuery()) {
                while (rs.next()) {
                    usage += rs.getLong("usageCount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usage;
    }

    public Map<Integer, Long> getUsages(long[] period, List<Integer> commandIds) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        Map<Integer, Long> sortedCommandUsages = new HashMap<>();
        Map<Integer, Long> commandUsages = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement getUsageWithinPeriod = con.prepareStatement("SELECT * FROM command_usage WHERE time < ? AND time > ?")) {
            getUsageWithinPeriod.setLong(1, biggest);
            getUsageWithinPeriod.setLong(2, smallest);
            try (ResultSet rs = getUsageWithinPeriod.executeQuery()) {
                while (rs.next()) {
                    if (commandIds.contains(rs.getInt("commandId")))
                        commandUsages.put(rs.getInt("commandId"),
                                (commandUsages.containsKey(rs.getInt("commandId")) ?
                                        commandUsages.get(rs.getInt("commandId")) :
                                        0
                                ) + rs.getLong("usageCount"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < commandUsages.size(); i++) {
            var ref = new Object() {
                int key = -1;
                long value = -1;
            };
            commandUsages.forEach((key, value) -> {
                if (value > ref.value) {
                    ref.key = key;
                    ref.value = value;
                }
            });
            if (ref.key != -1) {
                commandUsages.remove(ref.key);
                sortedCommandUsages.put(ref.key, ref.value);
            }
        }
        return sortedCommandUsages;
    }

    private <K, V extends Comparable<V>> LinkedHashMap<K, V> sortMetricsMap(Map<K, V> map, int n) {
        Comparator<Map.Entry<K, V>> comparator = (e0, e1) -> {
            V v0 = e0.getValue();
            V v1 = e1.getValue();
            return v0.compareTo(v1);
        };

        PriorityQueue<Map.Entry<K, V>> highest = new PriorityQueue<>(n, comparator);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            highest.offer(entry);
            while (highest.size() > n) {
                highest.poll();
            }
        }

        List<Map.Entry<K, V>> result = new ArrayList<>();
        while (highest.size() > 0) {
            result.add(highest.poll());
        }

        LinkedHashMap<K, V> coolMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : result) {
            coolMap.put(entry.getKey(), entry.getValue());
        }

        Comparator<Map.Entry<K, V>> comparator2 = (e0, e1) -> {
            V v0 = e0.getValue();
            V v1 = e1.getValue();
            return v1.compareTo(v0);
        };
        PriorityQueue<Map.Entry<K, V>> highest2 = new PriorityQueue<>(n, comparator2);
        for (Map.Entry<K, V> entry : coolMap.entrySet()) {
            highest2.offer(entry);
            while (highest2.size() > n) {
                highest2.poll();
            }
        }

        List<Map.Entry<K, V>> result2 = new ArrayList<>();
        while (highest2.size() > 0) {
            result2.add(highest2.poll());
        }

        LinkedHashMap<K, V> coolMap2 = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : result2) {
            coolMap2.put(entry.getKey(), entry.getValue());
        }
        return coolMap2;
    }

    public String getMySQLVersion() {
        try (Connection con = ds.getConnection()) {
            return con.getMetaData().getDatabaseProductVersion().replaceAll("(\\d+\\.\\d+\\.\\d+)-.*", "$1");
        } catch (SQLException e) {
            return null;
        }
    }

    public String getConnectorVersion() {
        try (Connection con = ds.getConnection()) {
            return con.getMetaData().getDriverVersion().replaceAll("mysql-connector-java-(\\d+\\.\\d+\\.\\d+).*", "$1");
        } catch (SQLException e) {
            return null;
        }
    }

    public void removeBan(Member member, long time) {
        executeUpdate("DELETE FROM history_bans WHERE guildId=? AND victimId=? AND startTime < ? AND startTime > ?",
                member.getGuild().getIdLong(), member.getUser().getIdLong(), time + 1000, time - 1000);
    }

    public void removeMute(Member member, long time) {
        executeUpdate("DELETE FROM history_mutes WHERE guildId=? AND victimId=? AND startTime < ? AND startTime > ?",
                member.getGuild().getIdLong(), member.getUser().getIdLong(), time + 1000, time - 1000);
    }

    public void removeWarn(Member member, long time) {
        executeUpdate("DELETE FROM warns WHERE guildId=? AND victimId=? AND moment < ? AND moment > ?",
                member.getGuild().getIdLong(), member.getUser().getIdLong(), time + 1000, time - 1000);
    }

    public void removeKick(Member member, long time) {
        executeUpdate("DELETE FROM kicks WHERE guildId=? AND victimId=? AND moment < ? AND moment > ?",
                member.getGuild().getIdLong(), member.getUser().getIdLong(), time + 1000, time - 1000);
    }

    public Map<Long, String> getSelfRoles(long guildId) {
        Map<Long, String> collector = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM self_roles WHERE guildId= ?")) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    collector.put(rs.getLong("roleId"), rs.getString("emote"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return collector;
    }

    public void addSelfRole(long guildId, long roleId, String emote) {
        executeUpdate("INSERT IGNORE INTO self_roles (guildId, roleId, emote) VALUES (?, ?, ?)", guildId, roleId, emote);
    }

    public void removeSelfRole(long guildId, long roleId) {
        executeUpdate("DELETE FROM self_roles WHERE guildId= ? AND roleId= ?", guildId, roleId);
    }

    public void removeSelfRole(long guildId, long roleId, String emote) {
        executeUpdate("DELETE FROM self_roles WHERE guildId= ? AND roleId= ? AND emote= ?", guildId, roleId, emote);
    }

    public JSONArray getCustomCommands(long guildId) {
        JSONArray toRet = new JSONArray();
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM custom_commands WHERE guildId= ?")) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    toRet.put(new JSONObject()
                            .put("name", rs.getString("name"))
                            .put("description", rs.getString("description"))
                            .put("aliases", rs.getString("aliases"))
                            .put("message", rs.getString("message"))
                            .put("attachment", rs.getString("attachment"))
                            .put("prefix", rs.getBoolean("prefix"))
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toRet;
    }

    public JSONObject getCustomCommand(long guildId, String name) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM custom_commands WHERE guildId= ? AND name= ? LIMIT 1")) {
            statement.setLong(1, guildId);
            statement.setString(2, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new JSONObject()
                            .put("name", rs.getString("name"))
                            .put("description", rs.getString("description"))
                            .put("aliases", rs.getString("aliases"))
                            .put("message", rs.getString("message"))
                            .put("attachment", rs.getString("attachment"))
                            .put("prefix", rs.getBoolean("prefix"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int addCustomCommand(long guildId, String name, String message) {
        if (getCustomCommands(guildId).length() >= CustomCommandCommand.limitCC) return 0;
        boolean b = executeUpdate("INSERT IGNORE INTO custom_commands (guildId, name, description, aliases, prefix, attachment, message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guildId, name, "", "", true, "", message) > 0;
        if (!b) {
            String msg = getCustomCommand(guildId, name).getString("message") + "%split%" + message;
            executeUpdate("UPDATE custom_commands SET message= ? WHERE guildId= ? AND name= ?", msg, guildId, name);
            return 2;
        }
        return 1;
    }

    public boolean removeCustomCommand(long guildId, String name) {
        return executeUpdate("DELETE FROM custom_commands WHERE guildId= ? AND name= ?", guildId, name) > 0;
    }

    public boolean removeCustomCommandMessage(long guildId, String name, String message) {
        String msg = getCustomCommand(guildId, name).getString("message").replaceFirst("(%split%)?" + message, "");
        return executeUpdate("UPDATE custom_commands SET message= ? WHERE guildId= ? AND name= ?", msg, guildId, name) > 0;
    }

    public boolean updateCustomCommand(long guildId, String name, String message) {
        return executeUpdate("UPDATE custom_commands SET message= ? WHERE guildId= ? AND name= ?", message, guildId, name) > 0;
    }

    public void updateCustomCommandPrefix(long guildId, String name, boolean prefix) {
        executeUpdate("UPDATE custom_commands SET prefix= ? WHERE guildId= ? AND name= ?", prefix, guildId, name);
    }

    public void updateCustomCommandDescription(long guildId, String name, String description) {
        executeUpdate("UPDATE custom_commands SET description= ? WHERE guildId= ? AND name= ?", description, guildId, name);
    }

    public void updateCustomCommandAttachment(long guildId, String name, String attachment) {
        executeUpdate("UPDATE custom_commands SET attachment= ? WHERE guildId= ? AND name= ?", attachment, guildId, name);
    }

    public void updateCustomCommandAliases(long guildId, String name, List<String> aliases) {
        executeUpdate("UPDATE custom_commands SET aliases= ? WHERE guildId= ? AND name= ?", String.join("%split%", aliases), guildId, name);
    }

    public void addQueue(long guildId, long channelId, boolean paused, Queue<AudioTrack> queue) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("INSERT INTO saved_queues (guildId, position, url) VALUES (?, ?, ?)")) {
            statement.setLong(1, guildId);
            statement.setInt(2, 0);
            statement.setString(3, channelId + "-" + paused);
            statement.addBatch();
            int i = 1;
            for (AudioTrack track : queue) {
                statement.setLong(1, guildId);
                statement.setInt(2, i++);
                statement.setString(3, track.getInfo().uri);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<JSONObject> getQueues() {
        List<JSONObject> queues = new ArrayList<>();

        //parses and adds basic info
        executeQuery("SELECT * FROM saved_queues", rs -> {
            try {
                while (rs.next()) {
                    if (rs.getInt("position") == 0) {
                        queues.add(new JSONObject()
                                .put("guildId", rs.getLong("guildId"))
                                .put("channelId", rs.getString("url").split("-")[0])
                                .put("paused", Boolean.parseBoolean(rs.getString("url").split("-")[1]))
                                .put("urls", ""));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        //Adds all urls
        for (JSONObject obj : queues) {
            executeQuery("SELECT * FROM saved_queues WHERE guildId= ?", rs -> {
                try {
                    while (rs.next()) {
                        if (rs.getInt("position") != 0)
                            obj.put("urls", obj.getString("urls") + "#" + rs.getInt("position") + " " + rs.getString("url") + "\n");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, obj.getLong("guildId"));
        }
        return queues;
    }

    public void clearQueues() {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE saved_queues");
            logger.info("Truncated queues");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Not truncated queues");
        }

    }

    public Integer getEmbedColor(Long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM embed_colors WHERE guildId= ? LIMIT 1")) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getInt("color");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return melijn.getVariables().embedColor;
    }

    public void setEmbedColor(long guildId, int color) {
        executeUpdate("INSERT INTO embed_colors (guildId, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color= ?", guildId, color, color);
    }

    public void removeEmbedColor(long guildId) {
        executeUpdate("DELETE FROM embed_colors WHERE guildId= ?", guildId);
    }

    public List<String> getPrivatePrefixes(long userId) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM private_prefixes WHERE userId= ? LIMIT 1")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next())
                    return rs.getString("prefixes").length() > 0 ?
                            Arrays.asList(rs.getString("prefixes").split("%split%")) :
                            new ArrayList<>();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void addPrivatePrefix(long userId, String prefix) {
        List<String> prefixes = new ArrayList<>(melijn.getVariables().privatePrefixes.getUnchecked(userId));
        prefixes.add(prefix);
        executeUpdate("INSERT INTO private_prefixes (userId, prefixes) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefixes= ?",
                userId, String.join("%split%", prefixes), String.join("%split%", prefixes));

    }

    public void removePrivatePrefix(long userId, String prefix) {
        List<String> prefixes = new ArrayList<>(melijn.getVariables().privatePrefixes.getUnchecked(userId));
        prefixes.remove(prefix);
        executeUpdate("INSERT INTO private_prefixes (userId, prefixes) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefixes= ?",
                userId, String.join("%split%", prefixes), String.join("%split%", prefixes));
    }

    public void setCooldown(long guildId, List<Command> commands, int cooldown) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("INSERT INTO cooldowns (guildId, commandId, cooldown) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE cooldown= ?")) {
            for (Command command : commands) {
                statement.setLong(1, guildId);
                statement.setInt(2, command.getId());
                statement.setInt(3, cooldown);
                statement.setInt(4, cooldown);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeCooldown(long guildId, List<Command> commands) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM cooldowns WHERE guildId= ? AND commandId= ?")) {
            for (Command command : commands) {
                statement.setLong(1, guildId);
                statement.setInt(2, command.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Integer> getCooldowns(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM cooldowns WHERE guildId= ?")) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                Map<Integer, Integer> map = new HashMap<>();
                while (rs.next()) {
                    map.put(rs.getInt("commandId"), rs.getInt("cooldown"));
                }
                return map;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }


    public VerificationType getVerificationType(long guildId) {
        try (Connection con = ds.getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM verification_types WHERE guildId=? LIMIT 1")) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next())
                    return VerificationType.valueOf(rs.getString("type"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return VerificationType.CODE;
    }

    public void setVerificationType(long guildId, VerificationType type) {
        executeUpdate("INSERT INTO verification_types (guildId, type) VALUES (?, ?) ON DUPLICATE KEY UPDATE type= ?",
                guildId, type.name(), type.name());
    }

    public void updateMessage(Message message) {
        executeUpdate("INSERT INTO history_messages (guildId, authorId, messageId, content, textChannelId, sentTime) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE content= ?",
                message.getGuild().getIdLong(),
                message.getAuthor().getIdLong(),
                message.getIdLong(),
                message.getContentRaw(),
                message.getTextChannel().getIdLong(),
                message.getCreationTime().toEpochSecond() * 1000,
                message.getContentRaw());

    }

    public void addForceRole(long guildId, long userId, long roleId) {
        executeUpdate("INSERT INTO forced_roles(guildId, userId, roleId) VALUES (?, ?, ?)",
                guildId, userId, roleId);
    }

    public void removeForceRole(long guildId, long userId, long roleId) {
        executeUpdate("DELETE FROM forced_roles WHERE guildId= ? AND userId= ? AND roleId= ?",
                guildId, userId, roleId);
    }

    public List<Long> getForcedRoles(long guildId, long userId) {
        List<Long> roles = new ArrayList<>();
        try (Connection con = ds.getConnection();
        PreparedStatement statement = con.prepareStatement("SELECT * FROM forced_roles WHERE guildId= ? AND userId= ?")) {
            statement.setLong(1, guildId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getLong("roleId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }
}
