package me.melijn.jda;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import me.melijn.jda.blub.CommandClientBuilder;
import me.melijn.jda.commands.DonateCommand;
import me.melijn.jda.commands.HelpCommand;
import me.melijn.jda.commands.InviteCommand;
import me.melijn.jda.commands.VoteCommand;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.developer.ShutdownCommand;
import me.melijn.jda.commands.developer.TestCommand;
import me.melijn.jda.commands.developer.WeebshCommand;
import me.melijn.jda.commands.fun.*;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.commands.music.*;
import me.melijn.jda.commands.util.*;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.events.AddReaction;
import me.melijn.jda.events.Channels;
import me.melijn.jda.events.Chat;
import me.melijn.jda.events.JoinLeave;
import me.melijn.jda.rest.Application;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import org.jooby.Jooby;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

public class Melijn {

    private static final Config config = Config.getConfigInstance();
    public static long OWNERID = Long.parseLong(config.getValue("ownerid"));
    public static String PREFIX = config.getValue("prefix");
    private static ShardManager shardManager;

    public static MySQL mySQL = new MySQL(
            config.getValue("ipaddress"),
            config.getValue("username"),
            config.getValue("password"),
            config.getValue("database"));

    public static void main(String[] args) throws LoginException {
        new WebUtils();
        mySQL.executeUpdate("TRUNCATE TABLE commands");
        CommandClientBuilder client = new CommandClientBuilder();
        client.setOwnerId(OWNERID);
        client.addCommands(new BirdCommand(),//Only add commands at the end of the list for because of commandIndexes
                new UrbanCommand(),
                new BlurpleCommand(),
                new InvertCommand(),
                new SetVerificationThreshold(),
                new SetUnverifiedRole(),
                new SetVerificationCode(),
                new SetVerificationChannel(),
                new ShardsCommand(),
                new ClearChannelCommand(),
                new NyanCatCommand(),
                new SummonCommand(),
                new ForwardCommand(),
                new RewindCommand(),
                new NightCoreCommand(),
                new TremoloCommand(),
                new PitchCommand(),
                new SpeedCommand(),
                new CryCommand(),
                new ShrugCommand(),
                new DabCommand(),
                new HighfiveCommand(),
                new WastedCommand(),
                new LewdCommand(),
                new PunchCommand(),
                new ShuffleCommand(),
                new EvalCommand(),
                new WeebshCommand(),
                new SayCommand(),
                new DiscordMemeCommand(),
                new LoopQueueCommand(),
                new SetNotifications(),
                new VoteCommand(),
                new InviteCommand(),
                new SetJoinLeaveChannelCommand(),
                new SetJoinRoleCommand(),
                new SetJoinMessageCommand(),
                new SetLeaveMessageCommand(),
                new TriggeredCommand(),
                new SlapCommand(),
                new PatCommand(),
                new FilterCommand(),
                new PotatoCommand(),
                new PauseCommand(),
                new SPlayCommand(),
                new BanCommand(),
                new HistoryCommand(),
                new MuteCommand(),
                new SetMuteRoleCommand(),
                new TempMuteCommand(),
                new UnmuteCommand(),
                new KickCommand(),
                new AvatarCommand(),
                new WarnCommand(),
                new PurgeCommand(),
                new HelpCommand(),
                new PingCommand(),
                new PlayCommand(),
                new QueueCommand(),
                new CatCommand(),
                new SkipCommand(),
                new ClearCommand(),
                new StopCommand(),
                new ResumeCommand(),
                new VolumeCommand(),
                new InfoCommand(),
                new UserInfoCommand(),
                new LoopCommand(),
                new TextToEmojiCommand(),
                new SeekCommand(),
                new PermCommand(),
                new NowPlayingCommand(),
                new RemoveCommand(),
                new GuildInfoCommand(),
                new RolesCommand(),
                new RoleCommand(),
                new DogCommand(),
                new SetPrefixCommand(),
                new SetMusicChannelCommand(),
                new SetLogChannelCommand(),
                new TempBanCommand(),
                new UnbanCommand(),
                new SetStreamerModeCommand(),
                new SetStreamUrlCommand(),
                new VerifyCommand(),
                new EnableCommand(),
                new DisableCommand(),
                new MetricsCommand(),
                new SettingsCommand(),
                new DonateCommand(),
                new SlowModeCommand(),
                new UnicodeCommand(),
                new AlpacaCommand(),
                new StatsCommand(),
                new KissCommand(),
                new HugCommand(),
                new SpookifyCommand(),
                new SelfRoleCommand(),
                new SetSelfRoleChannelCommand(),
                new CustomCommandCommand(),
                new ShutdownCommand(),
                new PollCommand(),
                new DiceCommand(),
                new TestCommand()
        );

        shardManager = new DefaultShardManagerBuilder()
                .setShardsTotal(Integer.parseInt(config.getValue("shardCount")))
                .setToken(config.getValue("token"))
                .setGame(Game.playing(PREFIX + "help | melijn.com"))
                .setAutoReconnect(true)
                .addEventListeners(client.build(), new JoinLeave(), new AddReaction(), new Channels(), new Chat())
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME))
                .setAudioSendFactory(new NativeAudioSendFactory())
                .build();


        EvalCommand.serverBlackList.add(new long[]{110373943822540800L, 264445053596991498L});
        EvalCommand.userBlackList.add(new long[]{/*fabian: 260424455270957058L*/
                //people who own bot farms
                244397405846372354L, 324570870800449548L, 444348640450969600L

                //people who spam the bot

        });
        TaskScheduler.async(() -> Jooby.run(Application::new, args));
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> MessageHelper.printException(thread, exception, null, null));
    }

    public static ShardManager getShardManager() {
        return shardManager;
    }
}
