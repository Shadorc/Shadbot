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
import me.shadorc.discordbot.command.game.DiceCmd;
import me.shadorc.discordbot.command.game.RpsCmd;
import me.shadorc.discordbot.command.game.RussianRouletteCmd;
import me.shadorc.discordbot.command.game.SlotMachineCmd;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.gamestats.CounterStrikeCmd;
import me.shadorc.discordbot.command.gamestats.DiabloCmd;
import me.shadorc.discordbot.command.gamestats.OverwatchCmd;
import me.shadorc.discordbot.command.hidden.HelpCmd;
import me.shadorc.discordbot.command.hidden.ReportCmd;
import me.shadorc.discordbot.command.image.GifCmd;
import me.shadorc.discordbot.command.image.ImageCmd;
import me.shadorc.discordbot.command.image.Rule34Cmd;
import me.shadorc.discordbot.command.image.SuicideGirlsCmd;
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
import me.shadorc.discordbot.command.owner.ShutdownCmd;
import me.shadorc.discordbot.command.utils.CalcCmd;
import me.shadorc.discordbot.command.utils.LyricsCmd;
import me.shadorc.discordbot.command.utils.PollCmd;
import me.shadorc.discordbot.command.utils.TranslateCmd;
import me.shadorc.discordbot.command.utils.UrbanCmd;
import me.shadorc.discordbot.command.utils.WeatherCmd;
import me.shadorc.discordbot.command.utils.WikiCmd;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Stats.Category;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandManager {

	private final static CommandManager COMMAND_MANAGER = new CommandManager();

	private final Map<String, AbstractCommand> commandsMap = new LinkedHashMap<>();

	public CommandManager() {
		this.register(
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
				// Image Commands
				new ImageCmd(),
				new SuicideGirlsCmd(),
				new Rule34Cmd(),
				new GifCmd(),
				// Games Commands
				new DiceCmd(),
				new SlotMachineCmd(),
				new RussianRouletteCmd(),
				new TriviaCmd(),
				new RpsCmd(),
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
				new ShutdownCmd());
	}

	private void register(AbstractCommand... cmds) {
		for(AbstractCommand command : cmds) {
			for(String name : command.getNames()) {
				if(commandsMap.containsKey(name)) {
					LogUtils.error("Command name collision between " + command.getClass() + " and " + commandsMap.get(name).getClass());
					continue;
				}
				commandsMap.put(name, command);
			}
		}
	}

	public void manage(MessageReceivedEvent event) {
		Context context = new Context(event);
		AbstractCommand command = commandsMap.get(context.getCommand());

		if(command == null) {
			Stats.increment(Category.UNKNOWN_COMMAND, context.getCommand());
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
			Stats.increment(Category.COMMAND, context.getCommand());
		} catch (MissingArgumentException err) {
			command.showHelp(context);
			Stats.increment(Category.HELP_COMMAND, context.getCommand());
		}
	}

	public AbstractCommand getCommand(String name) {
		return commandsMap.get(name);
	}

	public Map<String, AbstractCommand> getCommands() {
		return commandsMap;
	}

	public static CommandManager getInstance() {
		return COMMAND_MANAGER;
	}
}