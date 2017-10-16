package com.pixelatedsource.jda;

import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.pixelatedsource.jda.commands.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

public class PixelatedBot extends ListenerAdapter {

    static final Config config = new Config();
    public static String OWNERID = config.getValue("ownerid");
    public static String TOKEN = config.getValue("token");
    public static String PREFIX = config.getValue("prefix");


    public static void main(String[] args) throws LoginException, RateLimitedException {
        CommandClientBuilder client = new CommandClientBuilder();
        client.setEmojis("\uD83D\uDE03", "\uD83D\uDE2E", "\uD83D\uDE26");
        client.setOwnerId(OWNERID);
        client.setPrefix(PREFIX);
        client.addCommands(
                new PingCommand(),
                new PlayCommand(),
                new QueueCommand(),
                new CatCommand(),
                new SkipCommand(),
                new ClearCommand(),
                new StopCommand(),
                new ResumeCommand(),
                new VolumeCommand(),
                new AboutCommand(),
                new PlayerinfoCommand()
        );
        new JDABuilder(AccountType.BOT)
                .setToken(TOKEN)
                .setGame(Game.of(PREFIX + "help", "https://www.twitch.tv/pixelhamster"))
                .addEventListener(client.build())
                .buildAsync();
        Helpers.starttime = System.currentTimeMillis();
    }

    public void onDisconnect(DisconnectEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            guild.getAudioManager().closeAudioConnection();
        }
    }
}
