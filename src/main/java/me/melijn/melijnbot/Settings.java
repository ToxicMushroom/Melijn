package me.melijn.melijnbot;

import me.melijn.melijnbot.enums.Environment;

public class Settings {

    public String prefix;
    public long[] developerIds;
    public Environment environment;
    public int shardCount;
    public int restPort;
    public int embedColor;
    public long exceptionChannel;

    public Spotify spotify;
    public Lavalink lavalink;
    public Tokens tokens;
    public Database database;
    public String[] unLoggedThreads;

    public static class Spotify {
        public String clientId;
        public String password;
    }

    public static class Lavalink {
        public Node[] nodes;
        public boolean enabled;

        public static class Node {
            public String host;
            public String password;
        }
    }

    public static class Tokens {
        public String melijn;
        public String discordBotList;
        public String weebSh;
        public String cache;
    }

    public static class Database {
        public String database;
        public String password;
        public String user;
        public String host;
        public int port;
    }
}
