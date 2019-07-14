package me.melijn.melijnbot;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class MelijnBot {

    private static MelijnBot instance;

    private MelijnBot() throws IOException, LoginException {
        final Container container;

        container = new Container();


        ShardManager shardManager = new DefaultShardManagerBuilder()
                .setShardsTotal(container.settings.shardCount)
                .setToken(container.settings.token)
                .setActivity(Activity.listening("commands of users"))
                .setAutoReconnect(true)
                .addEventListeners()
                .build();

    }

    public static void main(String[] args) throws IOException, LoginException {
        instance = new MelijnBot();
    }

    public static MelijnBot getInstance() {
        return instance;
    }
}
