package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.admin.IamCmd;
import com.shadorc.shadbot.command.admin.ManageCoinsCmd;
import com.shadorc.shadbot.command.admin.PruneCmd;
import com.shadorc.shadbot.command.admin.SettingsCmd;
import com.shadorc.shadbot.command.admin.member.BanCmd;
import com.shadorc.shadbot.command.admin.member.KickCmd;
import com.shadorc.shadbot.command.admin.member.SoftBanCmd;
import com.shadorc.shadbot.command.currency.CoinsCmd;
import com.shadorc.shadbot.command.currency.LeaderboardCmd;
import com.shadorc.shadbot.command.currency.TransferCoinsCmd;
import com.shadorc.shadbot.command.fun.*;
import com.shadorc.shadbot.command.game.RussianRouletteCmd;
import com.shadorc.shadbot.command.game.blackjack.BlackjackCmd;
import com.shadorc.shadbot.command.game.dice.DiceCmd;
import com.shadorc.shadbot.command.game.hangman.HangmanCmd;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.game.roulette.RouletteCmd;
import com.shadorc.shadbot.command.game.rps.RpsCmd;
import com.shadorc.shadbot.command.game.slotmachine.SlotMachineCmd;
import com.shadorc.shadbot.command.game.trivia.TriviaCmd;
import com.shadorc.shadbot.command.gamestats.CounterStrikeCmd;
import com.shadorc.shadbot.command.gamestats.DiabloCmd;
import com.shadorc.shadbot.command.gamestats.FortniteCmd;
import com.shadorc.shadbot.command.gamestats.OverwatchCmd;
import com.shadorc.shadbot.command.hidden.*;
import com.shadorc.shadbot.command.image.*;
import com.shadorc.shadbot.command.info.*;
import com.shadorc.shadbot.command.music.*;
import com.shadorc.shadbot.command.owner.*;
import com.shadorc.shadbot.command.utils.*;
import com.shadorc.shadbot.command.utils.poll.PollCmd;
import com.shadorc.shadbot.utils.LogUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandManager {

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
                new ChatCmd(), new ThisDayCmd(), new LeetCmd(), new JokeCmd(), new DtcCmd(),
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
                new RolelistCmd(), new FeedbackCmd(),
                // Admin Commands
                new ManageCoinsCmd(), new PruneCmd(), new KickCmd(), new SoftBanCmd(), new BanCmd(),
                new IamCmd(), new SettingsCmd(),
                // Owner Commands
                new LoggerCmd(), new RestartCmd(), new LeaveCmd(),
                new GenerateRelicCmd(), new SendMessageCmd(), new ShutdownCmd(), new DatabaseCmd(),
                new EnableCommandCmd(), new StatsCmd(),
                // Hidden Commands
                new ActivateRelicCmd(), new HelpCmd(), new BaguetteCmd(), new RelicStatusCmd(), new PrefixCmd());
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
