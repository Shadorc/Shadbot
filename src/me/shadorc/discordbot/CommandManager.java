package me.shadorc.discordbot;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.command.HelpCmd;
import me.shadorc.discordbot.command.admin.AdminHelpCmd;
import me.shadorc.discordbot.command.admin.AllowsChannelCmd;
import me.shadorc.discordbot.command.admin.QuitCmd;
import me.shadorc.discordbot.command.fun.BashCmd;
import me.shadorc.discordbot.command.fun.ChatCmd;
import me.shadorc.discordbot.command.fun.GifCmd;
import me.shadorc.discordbot.command.fun.JokeCmd;
import me.shadorc.discordbot.command.game.CoinsCmd;
import me.shadorc.discordbot.command.game.RussianRouletteCmd;
import me.shadorc.discordbot.command.game.SlotMachineCmd;
import me.shadorc.discordbot.command.game.TransferCoinsCmd;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.music.MusicPlayCmd;
import me.shadorc.discordbot.command.music.NameCmd;
import me.shadorc.discordbot.command.music.NextCmd;
import me.shadorc.discordbot.command.music.PauseCmd;
import me.shadorc.discordbot.command.music.PlaylistCmd;
import me.shadorc.discordbot.command.music.StopCmd;
import me.shadorc.discordbot.command.music.VolumeCmd;
import me.shadorc.discordbot.command.utility.CalcCmd;
import me.shadorc.discordbot.command.utility.HolidaysCmd;
import me.shadorc.discordbot.command.utility.TranslateCmd;
import me.shadorc.discordbot.command.utility.WeatherCmd;
import me.shadorc.discordbot.command.utility.WikiCmd;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
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
				new RussianRouletteCmd(),
				new SlotMachineCmd(),
				new TriviaCmd(),
				new CoinsCmd(),
				new MusicPlayCmd(),
				new VolumeCmd(),
				new PauseCmd(),
				new StopCmd(),
				new NextCmd(),
				new NameCmd(),
				new PlaylistCmd(),
				new QuitCmd(),
				new AllowsChannelCmd()
				);
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

		if(!Utils.isChannelAllowed(context.getGuild(), context.getChannel()) && !context.isAuthorAdmin()) {
			return;
		}

		if(commands.containsKey(context.getCommand())) {
			Command command = commands.get(context.getCommand());
			if(command.isAdminCmd() && !context.isAuthorAdmin()) {
				BotUtils.sendMessage("Vous devez être administrateur pour exécuter cette commande.", event.getChannel());
			} else {
				command.execute(context);
			}
		} else {
			BotUtils.sendMessage("Cette commande n'existe pas, pour la liste des commandes disponibles, entrez /help.", event.getChannel());
			Log.warn("La commande " + context.getCommand() + " a été essayée sans résultat.");
		}
	}
}