package me.shadorc.discordbot;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.command.*;
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
				new WikiCommand()
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
		String command = event.getMessage().getContent().split(" ", 2)[0].replace("/", "").toLowerCase().trim();
		this.getCommand(command).execute(new Context(event));
	}

	public Command getCommand(String name) {
		return commands.get(name);
	}
}