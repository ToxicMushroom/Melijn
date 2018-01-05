package com.pixelatedsource.jda.db;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQL {

    private String ip;
    private String pass;
    private String user;
    private String dbname;
    private Connection con;

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
            con = DriverManager.getConnection("jdbc:mysql://" + this.ip + ":3306/" + this.dbname + "?autoReconnect=true&useUnicode=true",
                    this.user, this.pass);
            Statement statement = con.createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
            System.out.println("[MySQL] has connected");
            update("CREATE TABLE IF NOT EXISTS perms(guildName varchar(64), guildId varchar(128), roleName varchar(64), roleId varchar(128), permission varchar(256))");
        } catch (SQLException e) {
            System.out.println((char) 27 + "[31m" + "did not connect");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (this.con != null) {
                this.con.close();
                System.out.println("[MySQL] has disconnected");
            }
        } catch (SQLException e) {
            System.out.println("[MySQL] did not disconnect proparily error:" + e.getMessage());
        }
    }

    public void update(String qry) {
        try {
            Statement st = this.con.createStatement();
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
            Statement st = this.con.createStatement();
            rs = st.executeQuery(qry);
        } catch (SQLException e) {
            connect();
            e.printStackTrace();
        }
        return rs;
    }

    public void addPermission(Guild guild, Role role, String permission) {
        String id = role == null ? "all" : role.getId();
        String name = id.equals("all") ? "everyone" : role.getName();
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
        String id = role == null ? "all" : role.getId();
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
        String id = role == null ? "all" : role.getId();
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
        String id = role == null ? "all" : role.getId();
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
        String id = role == null ? "all" : role.getId();
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
}
