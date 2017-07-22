package me.shadorc.discordbot;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.command.*;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandManager {

	private final Map<String, Command> commands = new HashMap<>();

	public CommandManager() {
		this.register(
				new BashCommand(),
				new CalcCommand(),
				new ChatCommand(),
				new CoinsCommand(),
				new GifCommand(),
				new HelpCommand(),
				new HolidaysCommand(),
				new JokeCommand(),
				new RussianRouletteCommand(),
				new SlotMachineCommand(),
				new TranslateCommand(),
				new TriviaCommand(),
				new WeatherCommand(),
				new WikiCommand(),
				new MusicJoinCommand(),
				new MusicLeaveCommand(),
				new MusicPlayCommand()
				);
	}

	public void register(Command... cmds) {
		for(Command command : cmds) {
			for(String name : command.getNames()) {
				if(commands.containsKey(name)) {
					Log.error("Command name collision " + name + " in " + command.getClass().getName() + " !");
					continue;
				}
				commands.put(name, command);
			}
		}
	}

	public void manage(MessageReceivedEvent event) {
		String command = event.getMessage().getContent().split(" ", 2)[0].substring(1).toLowerCase().trim();
		if(commands.containsKey(command)) {
			commands.get(command).execute(new Context(event));
		} else {
			BotUtils.sendMessage("Cette commande n'existe pas, pour la liste des commandes disponibles, entrez /help.", event.getChannel());
			Log.info("La commande " + command + " a été essayée sans résultat.");
		}
	}
}