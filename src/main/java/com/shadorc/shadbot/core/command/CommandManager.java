package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.currency.CoinsCmd;
import com.shadorc.shadbot.command.currency.LeaderboardCmd;
import com.shadorc.shadbot.command.currency.TransferCoinsCmd;
import com.shadorc.shadbot.command.donator.ActivateRelicCmd;
import com.shadorc.shadbot.command.donator.RelicStatusCmd;
import com.shadorc.shadbot.command.fun.ChatCmd;
import com.shadorc.shadbot.command.fun.DtcCmd;
import com.shadorc.shadbot.command.fun.JokeCmd;
import com.shadorc.shadbot.command.fun.ThisDayCmd;
import com.shadorc.shadbot.command.game.rps.RpsCmd;
import com.shadorc.shadbot.command.game.russianroulette.RussianRouletteCmd;
import com.shadorc.shadbot.command.game.slotmachine.SlotMachineCmd;
import com.shadorc.shadbot.command.hidden.BaguetteCmd;
import com.shadorc.shadbot.command.image.*;
import com.shadorc.shadbot.command.info.*;
import com.shadorc.shadbot.command.music.*;
import com.shadorc.shadbot.command.owner.*;
import com.shadorc.shadbot.command.owner.shutdown.ShutdownCmd;
import com.shadorc.shadbot.command.utils.LyricsCmd;
import com.shadorc.shadbot.command.utils.MathCmd;
import com.shadorc.shadbot.command.utils.UrbanCmd;
import com.shadorc.shadbot.command.utils.WikipediaCmd;
import com.shadorc.shadbot.data.Config;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                /*new WeatherCmd(),*/ new MathCmd(), /*new TranslateCmd(),*/ new WikipediaCmd(), /*new PollCmd(),*/
                new UrbanCmd(), new LyricsCmd(),
                // Fun Commands
                new ChatCmd(), new ThisDayCmd(), new JokeCmd(), new DtcCmd(),
                // Image Commands
                new GifCmd(), new ImageCmd(), new WallpaperCmd(), new SuicideGirlsCmd(),
                new Rule34Cmd(), new XkcdCmd(),
                // Game Commands
                new RpsCmd(), /*new HangmanCmd(), new TriviaCmd(),*/ new RussianRouletteCmd(),
                new SlotMachineCmd(), /*new RouletteCmd(), new BlackjackCmd(), new DiceCmd(),*/
                /*new LotteryCmd(),*/
                // Currency Commands
                new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd(),
                // Music Commands
                /*new PlayCmd(),*/ new PauseCmd(), new StopCmd(), new SkipCmd(), new RepeatCmd(),
                new BackwardCmd(), new ForwardCmd(), new VolumeCmd(), new NameCmd(),
                new PlaylistCmd(), new ShuffleCmd(), new ClearCmd(), new BassBoostCmd(),
                // Game Stats Commands
//                new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd(), new OverwatchCmd(),
                // Info Commands
                new HelpCmd(), new PingCmd(), new InfoCmd(), new UserInfoCmd(), new ServerInfoCmd(),
                /*new RolelistCmd(),*/ new FeedbackCmd(), new InviteCmd(), new AchievementsCmd(),
                new VoteCmd(),
                // Admin Commands
//                new ManageCoinsCmd(), new PruneCmd(), new KickCmd(), new SoftBanCmd(), new BanCmd(),
//                new IamCmd(), new SettingsCmd(),
                // Owner Commands
                new LoggerCmd(), new LeaveGuildCmd(), new GenerateRelicCmd(), new SendMessageCmd(), new ShutdownCmd(),
                new EnableCommandCmd(), new ManageAchievementsCmd(),
                // Donator Commands
                new ActivateRelicCmd(), new RelicStatusCmd());
    }

    private static Map<String, BaseCmd> initialize(BaseCmd... cmds) {
        final Map<String, BaseCmd> map = new LinkedHashMap<>();
        for (final BaseCmd cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                DEFAULT_LOGGER.error("Command name collision between {} and {}",
                        cmd.getClass().getSimpleName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        DEFAULT_LOGGER.info("{} commands initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    public Mono<Long> register(RestClient restClient, long applicationId) {
        return Flux.fromIterable(this.commandsMap.values())
                .flatMap(cmd -> this.registerCommand(applicationId, restClient, cmd)
                        .onErrorResume(err -> Mono.fromRunnable(() ->
                                DEFAULT_LOGGER.error("An error occurred during '{}' registration: {}", cmd.getName(), err.getMessage()))))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} commands registered", cmdCount));
    }

    private Mono<ApplicationCommandData> registerCommand(long applicationId, RestClient restClient, BaseCmd cmd) {
        final ApplicationCommandRequest request = cmd.build(ApplicationCommandRequest.builder()
                .name(cmd.getName())
                .description(cmd.getDescription()));
        if (cmd.getPermission().equals(CommandPermission.OWNER)) {
            return restClient.getApplicationService()
                    .createGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, request);
        } else {
            // TODO
            // return restClient.getApplicationService()
            //       .createGlobalApplicationCommand(applicationId, request);
            return restClient.getApplicationService()
                    .createGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, request);
        }
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
