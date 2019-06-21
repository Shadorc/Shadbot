package me.shadorc.shadbot.core.command;

import me.shadorc.shadbot.command.admin.IamCmd;
import me.shadorc.shadbot.command.admin.ManageCoinsCmd;
import me.shadorc.shadbot.command.admin.PruneCmd;
import me.shadorc.shadbot.command.admin.SettingsCmd;
import me.shadorc.shadbot.command.admin.member.BanCmd;
import me.shadorc.shadbot.command.admin.member.KickCmd;
import me.shadorc.shadbot.command.admin.member.SoftBanCmd;
import me.shadorc.shadbot.command.currency.CoinsCmd;
import me.shadorc.shadbot.command.currency.LeaderboardCmd;
import me.shadorc.shadbot.command.currency.TransferCoinsCmd;
import me.shadorc.shadbot.command.french.DtcCmd;
import me.shadorc.shadbot.command.french.JokeCmd;
import me.shadorc.shadbot.command.fun.ChatCmd;
import me.shadorc.shadbot.command.fun.LeetCmd;
import me.shadorc.shadbot.command.fun.ThisDayCmd;
import me.shadorc.shadbot.command.game.LotteryCmd;
import me.shadorc.shadbot.command.game.RussianRouletteCmd;
import me.shadorc.shadbot.command.game.blackjack.BlackjackCmd;
import me.shadorc.shadbot.command.game.dice.DiceCmd;
import me.shadorc.shadbot.command.game.hangman.HangmanCmd;
import me.shadorc.shadbot.command.game.roulette.RouletteCmd;
import me.shadorc.shadbot.command.game.rps.RpsCmd;
import me.shadorc.shadbot.command.game.slotmachine.SlotMachineCmd;
import me.shadorc.shadbot.command.game.trivia.TriviaCmd;
import me.shadorc.shadbot.command.gamestats.CounterStrikeCmd;
import me.shadorc.shadbot.command.gamestats.DiabloCmd;
import me.shadorc.shadbot.command.gamestats.FortniteCmd;
import me.shadorc.shadbot.command.gamestats.OverwatchCmd;
import me.shadorc.shadbot.command.hidden.ActivateRelicCmd;
import me.shadorc.shadbot.command.hidden.BaguetteCmd;
import me.shadorc.shadbot.command.hidden.HelpCmd;
import me.shadorc.shadbot.command.hidden.RelicStatusCmd;
import me.shadorc.shadbot.command.image.*;
import me.shadorc.shadbot.command.info.*;
import me.shadorc.shadbot.command.music.*;
import me.shadorc.shadbot.command.owner.*;
import me.shadorc.shadbot.command.utils.*;
import me.shadorc.shadbot.command.utils.poll.PollCmd;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandManager {

    private static CommandManager instance;

    static {
        CommandManager.instance = new CommandManager();
    }

    private final Map<String, BaseCmd> commandsMap;

    private CommandManager() {
        this.commandsMap = new LinkedHashMap<>();
        this.add(
                // Utility Commands
                new WeatherCmd(), new CalcCmd(), new TranslateCmd(), new WikiCmd(), new PollCmd(),
                new UrbanCmd(), new LyricsCmd(),
                // Fun Commands
                new ChatCmd(), new ThisDayCmd(), new LeetCmd(),
                // Image Commands
                new GifCmd(), new ImageCmd(), new WallpaperCmd(), new SuicideGirlsCmd(),
                new Rule34Cmd(),
                // Game Commands
                new RpsCmd(), new HangmanCmd(), new TriviaCmd(), new RussianRouletteCmd(),
                new SlotMachineCmd(), new RouletteCmd(), new BlackjackCmd(), new DiceCmd(),
                new LotteryCmd(),
                // Currency Commands
                new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd(),
                // Music Commands
                new PlayCmd(), new PauseCmd(), new StopCmd(), new SkipCmd(), new RepeatCmd(),
                new BackwardCmd(), new ForwardCmd(), new VolumeCmd(), new NameCmd(),
                new PlaylistCmd(), new ShuffleCmd(), new ClearCmd(),
                // Game Stats Commands
                new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd(), new OverwatchCmd(),
                // Info Commands
                new PingCmd(), new InfoCmd(), new UserInfoCmd(), new ServerInfoCmd(),
                new RolelistCmd(),
                // French Commands
                new JokeCmd(), new DtcCmd(),
                // Admin Commands
                new ManageCoinsCmd(), new PruneCmd(), new KickCmd(), new SoftBanCmd(), new BanCmd(),
                new IamCmd(), new SettingsCmd(),
                // Owner Commands
                new LoggerCmd(), new StatsCmd(), new RestartCmd(), new LeaveCmd(),
                new GenerateRelicCmd(), new SendMessageCmd(), new ShutdownCmd(), new DatabaseCmd(),
                new CleanDatabaseCmd(),
                // Hidden Commands
                new ActivateRelicCmd(), new HelpCmd(), new BaguetteCmd(), new RelicStatusCmd());
    }

    private void add(BaseCmd... cmds) {
        for (final BaseCmd cmd : cmds) {
            for (final String name : cmd.getNames()) {
                if (this.commandsMap.putIfAbsent(name, cmd) != null) {
                    LogUtils.error(String.format("Command name collision between %s and %s.",
                            name, this.commandsMap.get(name).getClass().getSimpleName()));
                }
            }
        }
        LogUtils.info("%d commands initialized.", cmds.length);
    }

    public Map<String, BaseCmd> getCommands() {
        return Collections.unmodifiableMap(this.commandsMap);
    }

    public BaseCmd getCommand(String name) {
        return this.commandsMap.get(name);
    }

    public static CommandManager getInstance() {
        return CommandManager.instance;
    }
}
