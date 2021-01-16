package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.command.currency.CoinsCmd;
import com.shadorc.shadbot.command.currency.TransferCoinsCmd;
import com.shadorc.shadbot.command.fun.ChatCmd;
import com.shadorc.shadbot.command.image.GifCmd;
import com.shadorc.shadbot.command.image.ImageCmd;
import com.shadorc.shadbot.command.image.Rule34Cmd;
import com.shadorc.shadbot.command.image.XkcdCmd;
import com.shadorc.shadbot.command.music.*;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class CommandManager {

    private static CommandManager instance;

    static {
        CommandManager.instance = new CommandManager();
    }

    private final Map<String, BaseCmd> commandsMap;

    private CommandManager() {
        this.commandsMap = CommandManager.initialize(
                // Utility Commands
//                new WeatherCmd(), new MathCmd(), new TranslateCmd(), new WikipediaCmd(), new PollCmd(),
//                new UrbanCmd(), new LyricsCmd(),
                // Fun Commands
                new ChatCmd()/*, new ThisDayCmd(), new JokeCmd(), new DtcCmd()*/,
                // Image Commands
                new GifCmd(), new ImageCmd(),/*, new WallpaperCmd(), new SuicideGirlsCmd(),*/
                new Rule34Cmd(), new XkcdCmd(),
                // Game Commands
//                new RpsCmd(), new HangmanCmd(), new TriviaCmd(), new RussianRouletteCmd(),
//                new SlotMachineCmd(), new RouletteCmd(), new BlackjackCmd(), new DiceCmd(),
//                new LotteryCmd(),
                // Currency Commands
                new CoinsCmd(), /*new LeaderboardCmd(),*/ new TransferCoinsCmd(),
                // Music Commands
                /*new PlayCmd(),*/ new PauseCmd(), new StopCmd(), /*new SkipCmd(), new RepeatCmd(),*/
                new BackwardCmd(), new ForwardCmd(), /*new VolumeCmd(),*/ new NameCmd(),
                /*new PlaylistCmd(),*/ new ShuffleCmd(), new ClearCmd()/*, new BassBoostCmd()*/
                // Game Stats Commands
//                new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd(), new OverwatchCmd(),
                // Info Commands
//                new PingCmd(), new InfoCmd(), new UserInfoCmd(), new ServerInfoCmd(),
//                new RolelistCmd(), new FeedbackCmd(), new InviteCmd(), new AchievementsCmd(),
//                new VoteCmd(),
                // Admin Commands
//                new ManageCoinsCmd(), new PruneCmd(), new KickCmd(), new SoftBanCmd(), new BanCmd(),
//                new IamCmd(), new SettingsCmd(),
                // Owner Commands
//                new LoggerCmd(), new LeaveGuildCmd(), new GenerateRelicCmd(), new SendMessageCmd(), new ShutdownCmd(),
//                new EnableCommandCmd(), new ManageAchievementsCmd(),
                // Hidden Commands
                /*new ActivateRelicCmd(), new HelpCmd(), new BaguetteCmd(), new RelicStatusCmd(), new PrefixCmd()*/);
    }

    private static Map<String, BaseCmd> initialize(BaseCmd... cmds) {
        final Map<String, BaseCmd> map = new LinkedHashMap<>();
        for (final BaseCmd cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                DEFAULT_LOGGER.error("Command name collision between {} and {}",
                        cmd.getName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        DEFAULT_LOGGER.info("{} commands initialized", cmds.length);
        return Collections.unmodifiableMap(map);
    }

    // TODO
    public Flux<ApplicationCommandData> register(RestClient restClient) {
        final long applicationId = Shadbot.getApplicationId().asLong();
        final long guildId = 339318275320184833L;
        return Flux.fromIterable(this.commandsMap.values())
                .flatMap(cmd -> restClient.getApplicationService()
                        .createGuildApplicationCommand(applicationId, guildId,
                                cmd.build(ApplicationCommandRequest.builder()
                                        .name(cmd.getName())
                                        .description(cmd.getDescription()))));
    }

    public Map<String, BaseCmd> getCommands() {
        return this.commandsMap;
    }

    public BaseCmd getCommand(String name) {
        return this.commandsMap.get(name);
    }

    public static CommandManager getInstance() {
        return CommandManager.instance;
    }
}
