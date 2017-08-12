package me.shadorc.discordbot.command;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.admin.AllowsChannelCmd;
import me.shadorc.discordbot.command.currency.CoinsCmd;
import me.shadorc.discordbot.command.currency.LeaderboardCmd;
import me.shadorc.discordbot.command.currency.TransferCoinsCmd;
import me.shadorc.discordbot.command.french.BashCmd;
import me.shadorc.discordbot.command.french.HolidaysCmd;
import me.shadorc.discordbot.command.french.JokeCmd;
import me.shadorc.discordbot.command.fun.ChatCmd;
import me.shadorc.discordbot.command.fun.GifCmd;
import me.shadorc.discordbot.command.fun.ImageCmd;
import me.shadorc.discordbot.command.game.DiceCmd;
import me.shadorc.discordbot.command.game.RussianRouletteCmd;
import me.shadorc.discordbot.command.game.SlotMachineCmd;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.gamestats.CounterStrikeCmd;
import me.shadorc.discordbot.command.gamestats.OverwatchCmd;
import me.shadorc.discordbot.command.info.HelpCmd;
import me.shadorc.discordbot.command.info.InfoCmd;
import me.shadorc.discordbot.command.info.PingCmd;
import me.shadorc.discordbot.command.info.SuggestCmd;
import me.shadorc.discordbot.command.music.NameCmd;
import me.shadorc.discordbot.command.music.NextCmd;
import me.shadorc.discordbot.command.music.PauseCmd;
import me.shadorc.discordbot.command.music.PlayCmd;
import me.shadorc.discordbot.command.music.PlaylistCmd;
import me.shadorc.discordbot.command.music.RepeatCmd;
import me.shadorc.discordbot.command.music.StopCmd;
import me.shadorc.discordbot.command.music.VolumeCmd;
import me.shadorc.discordbot.command.utils.CalcCmd;
import me.shadorc.discordbot.command.utils.TranslateCmd;
import me.shadorc.discordbot.command.utils.UrbanCmd;
import me.shadorc.discordbot.command.utils.WeatherCmd;
import me.shadorc.discordbot.command.utils.WikiCmd;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandManager {

	private final static CommandManager COMMAND_MANAGER = new CommandManager();

	private final Map<String, Command> commandsMap = new HashMap<>();

	public CommandManager() {
		this.register(
				new HelpCmd(),
				// Utils Commands
				new TranslateCmd(),
				new WikiCmd(),
				new CalcCmd(),
				new WeatherCmd(),
				new UrbanCmd(),
				// Fun Commands
				new ChatCmd(),
				new GifCmd(),
				new ImageCmd(),
				// Games Commands
				new DiceCmd(),
				new SlotMachineCmd(),
				new RussianRouletteCmd(),
				new TriviaCmd(),
				// Currency Commands
				new TransferCoinsCmd(),
				new LeaderboardCmd(),
				new CoinsCmd(),
				// Music Commands
				new PlayCmd(),
				new VolumeCmd(),
				new PauseCmd(),
				new RepeatCmd(),
				new StopCmd(),
				new NextCmd(),
				new NameCmd(),
				new PlaylistCmd(),
				// Games Stats Commands
				new OverwatchCmd(),
				new CounterStrikeCmd(),
				// Info Commands
				new InfoCmd(),
				new PingCmd(),
				new SuggestCmd(),
				// French Commands
				new BashCmd(),
				new JokeCmd(),
				new HolidaysCmd(),
				// Admin Commands
				new AllowsChannelCmd());
	}

	private void register(Command... cmds) {
		for(Command command : cmds) {
			for(String name : command.getNames()) {
				if(commandsMap.containsKey(name)) {
					Log.warn("Command name collision between " + command.getClass() + " and " + commandsMap.get(name).getClass());
					continue;
				}
				commandsMap.put(name, command);
			}
		}
	}

	public void manage(MessageReceivedEvent event) {
		Context context = new Context(event);

		if(!BotUtils.isChannelAllowed(context.getGuild(), context.getChannel())) {
			return;
		}

		if(!commandsMap.containsKey(context.getCommand())) {
			Log.warn("Guild \"" + context.getGuild().getName() + "\" (ID: " + context.getGuild().getStringID() + ") - Command not found : \"" + context.getCommand() + "\".");
			return;
		}

		Command command = commandsMap.get(context.getCommand());

		if(command.isAdminCmd() && !context.isAuthorAdmin()) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You have to be an administrator to execute this command.", event.getChannel());
			return;
		}

		try {
			command.execute(context);
		} catch (MissingArgumentException e) {
			command.showHelp(context);
		}
	}

	public Command getCommand(String name) {
		return commandsMap.containsKey(name) ? commandsMap.get(name) : null;
	}

	public static CommandManager getInstance() {
		return COMMAND_MANAGER;
	}
}