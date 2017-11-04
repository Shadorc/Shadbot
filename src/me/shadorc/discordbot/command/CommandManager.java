package me.shadorc.discordbot.command;

import java.util.LinkedHashMap;
import java.util.Map;

import me.shadorc.discordbot.command.admin.PruneCmd;
import me.shadorc.discordbot.command.admin.SettingsManagerCmd;
import me.shadorc.discordbot.command.currency.CoinsCmd;
import me.shadorc.discordbot.command.currency.LeaderboardCmd;
import me.shadorc.discordbot.command.currency.TransferCoinsCmd;
import me.shadorc.discordbot.command.french.DtcCmd;
import me.shadorc.discordbot.command.french.HolidaysCmd;
import me.shadorc.discordbot.command.french.JokeCmd;
import me.shadorc.discordbot.command.fun.ChatCmd;
import me.shadorc.discordbot.command.fun.LeetCmd;
import me.shadorc.discordbot.command.fun.ThisDayCmd;
import me.shadorc.discordbot.command.game.DiceCmd;
import me.shadorc.discordbot.command.game.LottoCmd;
import me.shadorc.discordbot.command.game.RouletteCmd;
import me.shadorc.discordbot.command.game.RpsCmd;
import me.shadorc.discordbot.command.game.RussianRouletteCmd;
import me.shadorc.discordbot.command.game.SlotMachineCmd;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.game.blackjack.BlackjackCmd;
import me.shadorc.discordbot.command.gamestats.CounterStrikeCmd;
import me.shadorc.discordbot.command.gamestats.DiabloCmd;
import me.shadorc.discordbot.command.gamestats.OverwatchCmd;
import me.shadorc.discordbot.command.hidden.HelpCmd;
import me.shadorc.discordbot.command.hidden.ReportCmd;
import me.shadorc.discordbot.command.image.GifCmd;
import me.shadorc.discordbot.command.image.ImageCmd;
import me.shadorc.discordbot.command.image.Rule34Cmd;
import me.shadorc.discordbot.command.image.SuicideGirlsCmd;
import me.shadorc.discordbot.command.image.WallpaperCmd;
import me.shadorc.discordbot.command.info.InfoCmd;
import me.shadorc.discordbot.command.info.PingCmd;
import me.shadorc.discordbot.command.info.ServerInfoCmd;
import me.shadorc.discordbot.command.info.UserInfoCmd;
import me.shadorc.discordbot.command.music.BackwardCmd;
import me.shadorc.discordbot.command.music.ClearCmd;
import me.shadorc.discordbot.command.music.ForwardCmd;
import me.shadorc.discordbot.command.music.NameCmd;
import me.shadorc.discordbot.command.music.PauseCmd;
import me.shadorc.discordbot.command.music.PlayCmd;
import me.shadorc.discordbot.command.music.PlaylistCmd;
import me.shadorc.discordbot.command.music.RepeatCmd;
import me.shadorc.discordbot.command.music.ShuffleCmd;
import me.shadorc.discordbot.command.music.SkipCmd;
import me.shadorc.discordbot.command.music.StopCmd;
import me.shadorc.discordbot.command.music.VolumeCmd;
import me.shadorc.discordbot.command.owner.AddCoinsCmd;
import me.shadorc.discordbot.command.owner.SendMessageCmd;
import me.shadorc.discordbot.command.owner.ShutdownCmd;
import me.shadorc.discordbot.command.owner.StatsCmd;
import me.shadorc.discordbot.command.utils.CalcCmd;
import me.shadorc.discordbot.command.utils.LyricsCmd;
import me.shadorc.discordbot.command.utils.PollCmd;
import me.shadorc.discordbot.command.utils.TranslateCmd;
import me.shadorc.discordbot.command.utils.UrbanCmd;
import me.shadorc.discordbot.command.utils.WeatherCmd;
import me.shadorc.discordbot.command.utils.WikiCmd;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandManager {

	private final static Map<String, AbstractCommand> COMMANDS_MAP = new LinkedHashMap<>();

	static {
		CommandManager.register(
				// Hidden Commands
				new HelpCmd(),
				new ReportCmd(),
				// Utils Commands
				new TranslateCmd(),
				new WikiCmd(),
				new CalcCmd(),
				new WeatherCmd(),
				new UrbanCmd(),
				new PollCmd(),
				new LyricsCmd(),
				// Fun Commands
				new ChatCmd(),
				new LeetCmd(),
				new ThisDayCmd(),
				// Image Commands
				new ImageCmd(),
				new SuicideGirlsCmd(),
				new Rule34Cmd(),
				new GifCmd(),
				new WallpaperCmd(),
				// Games Commands
				new DiceCmd(),
				new SlotMachineCmd(),
				new RussianRouletteCmd(),
				new TriviaCmd(),
				new RpsCmd(),
				new RouletteCmd(),
				new BlackjackCmd(),
				new LottoCmd(),
				// Currency Commands
				new TransferCoinsCmd(),
				new LeaderboardCmd(),
				new CoinsCmd(),
				// Music Commands
				new PlayCmd(),
				new PauseCmd(),
				new RepeatCmd(),
				new StopCmd(),
				new VolumeCmd(),
				new SkipCmd(),
				new BackwardCmd(),
				new ForwardCmd(),
				new NameCmd(),
				new PlaylistCmd(),
				new ClearCmd(),
				new ShuffleCmd(),
				// Games Stats Commands
				new OverwatchCmd(),
				new DiabloCmd(),
				new CounterStrikeCmd(),
				// Info Commands
				new InfoCmd(),
				new UserInfoCmd(),
				new ServerInfoCmd(),
				new PingCmd(),
				// French Commands
				new DtcCmd(),
				new JokeCmd(),
				new HolidaysCmd(),
				// Admin Commands
				new SettingsManagerCmd(),
				new PruneCmd(),
				// Owner Commands
				new SendMessageCmd(),
				new ShutdownCmd(),
				new AddCoinsCmd(),
				new StatsCmd());
	}

	private static void register(AbstractCommand... cmds) {
		for(AbstractCommand command : cmds) {
			for(String name : command.getNames()) {
				if(COMMANDS_MAP.containsKey(name)) {
					LogUtils.error("Command name collision between " + command.getClass().getSimpleName()
							+ " and " + COMMANDS_MAP.get(name).getClass().getSimpleName());
					continue;
				}
				COMMANDS_MAP.put(name, command);
			}
		}
	}

	public static void manage(MessageReceivedEvent event) {
		Context context = new Context(event);
		AbstractCommand command = COMMANDS_MAP.get(context.getCommand());

		if(command == null) {
			return;
		}

		if(!BotUtils.isCommandAllowed(context.getGuild(), command)) {
			return;
		}

		Role authorRole = context.getAuthorRole();
		if(command.getRole().equals(Role.OWNER) && !authorRole.equals(Role.OWNER)) {
			return;
		}

		if(command.getRole().equals(Role.ADMIN) && !authorRole.equals(Role.ADMIN) && !authorRole.equals(Role.OWNER)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You have to be an administrator to execute this command.", event.getChannel());
			return;
		}

		try {
			command.execute(context);
			StatsManager.increment(StatCategory.COMMAND, context.getCommand());
		} catch (MissingArgumentException err) {
			command.showHelp(context);
			StatsManager.increment(StatCategory.HELP_COMMAND, context.getCommand());
		}
	}

	public static Map<String, AbstractCommand> getCommands() {
		return COMMANDS_MAP;
	}

	public static AbstractCommand getCommand(String name) {
		return COMMANDS_MAP.get(name);
	}
}