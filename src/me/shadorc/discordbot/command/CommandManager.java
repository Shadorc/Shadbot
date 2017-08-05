package me.shadorc.discordbot.command;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.admin.AdminHelpCmd;
import me.shadorc.discordbot.command.admin.AllowsChannelCmd;
import me.shadorc.discordbot.command.admin.DebugCmd;
import me.shadorc.discordbot.command.currency.CoinsCmd;
import me.shadorc.discordbot.command.currency.LeaderboardCmd;
import me.shadorc.discordbot.command.currency.TransferCoinsCmd;
import me.shadorc.discordbot.command.french.BashCmd;
import me.shadorc.discordbot.command.french.HolidaysCmd;
import me.shadorc.discordbot.command.french.JokeCmd;
import me.shadorc.discordbot.command.fun.ChatCmd;
import me.shadorc.discordbot.command.fun.GifCmd;
import me.shadorc.discordbot.command.game.DiceCmd;
import me.shadorc.discordbot.command.game.RussianRouletteCmd;
import me.shadorc.discordbot.command.game.SlotMachineCmd;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.info.CounterStrikeCmd;
import me.shadorc.discordbot.command.info.HelpCmd;
import me.shadorc.discordbot.command.info.InfoCmd;
import me.shadorc.discordbot.command.info.OverwatchCmd;
import me.shadorc.discordbot.command.info.PingCmd;
import me.shadorc.discordbot.command.music.NameCmd;
import me.shadorc.discordbot.command.music.NextCmd;
import me.shadorc.discordbot.command.music.PauseCmd;
import me.shadorc.discordbot.command.music.PlayCmd;
import me.shadorc.discordbot.command.music.PlaylistCmd;
import me.shadorc.discordbot.command.music.RepeatCmd;
import me.shadorc.discordbot.command.music.StopCmd;
import me.shadorc.discordbot.command.music.VolumeCmd;
import me.shadorc.discordbot.command.rpg.CharacterCmd;
import me.shadorc.discordbot.command.rpg.FightCmd;
import me.shadorc.discordbot.command.utils.CalcCmd;
import me.shadorc.discordbot.command.utils.TranslateCmd;
import me.shadorc.discordbot.command.utils.WeatherCmd;
import me.shadorc.discordbot.command.utils.WikiCmd;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandManager {

	private final Map<String, Command> commands = new HashMap<>();

	public CommandManager() {
		this.register(
				new HelpCmd(),
				new AdminHelpCmd(),
				new TranslateCmd(),
				new WikiCmd(),
				new HolidaysCmd(),
				new CalcCmd(),
				new WeatherCmd(),
				new ChatCmd(),
				new GifCmd(),
				new BashCmd(),
				new JokeCmd(),
				new TransferCoinsCmd(),
				new DiceCmd(),
				new RussianRouletteCmd(),
				new SlotMachineCmd(),
				new TriviaCmd(),
				new CoinsCmd(),
				new PlayCmd(),
				new VolumeCmd(),
				new PauseCmd(),
				new StopCmd(),
				new NextCmd(),
				new NameCmd(),
				new PlaylistCmd(),
				new RepeatCmd(),
				new AllowsChannelCmd(),
				new LeaderboardCmd(),
				new PingCmd(),
				new OverwatchCmd(),
				new CounterStrikeCmd(),
				new DebugCmd(),
				new CharacterCmd(),
				new FightCmd(),
				new InfoCmd());
	}

	private void register(Command... cmds) {
		for(Command command : cmds) {
			for(String name : command.getNames()) {
				if(commands.containsKey(name)) {
					Log.warn("Command name collision " + name + " in " + command.getClass().getName());
					continue;
				}
				commands.put(name, command);
			}
		}
	}

	public void manage(MessageReceivedEvent event) {
		Context context = new Context(event);

		if(!BotUtils.isChannelAllowed(context.getGuild(), context.getChannel())) {
			return;
		}

		if(context.getCommand().equals("help") && context.getArg() != null && commands.containsKey(context.getArg().replace("/", ""))) {
			commands.get(context.getArg()).showHelp(context);
			return;
		}

		if(commands.containsKey(context.getCommand())) {
			Command command = commands.get(context.getCommand());
			if(command.isAdminCmd() && !context.isAuthorAdmin()) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You have to be an administrator to execute this command.", event.getChannel());
			} else {
				try {
					command.execute(context);
				} catch (IllegalArgumentException e) {
					command.showHelp(context);
				}
			}
		} else {
			Log.warn("Command \"" + context.getCommand() + "\" has been tried without result.");
		}
	}
}