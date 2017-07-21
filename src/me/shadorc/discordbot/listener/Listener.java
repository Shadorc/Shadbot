package me.shadorc.discordbot.listener;

import java.util.List;

import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.command.TriviaCommand;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class Listener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		Log.print("\n---------------- Shadbot is connected [DEBUG MODE : " + Main.DEBUG + "] ----------------");
		Log.print("ShadBot is connected to :");
		List <IGuild> guilds = event.getClient().getGuilds();
		for(IGuild guild : guilds) {
			Log.print("\t*Guild: " + guild.getName() + " (ID: " + guild.getLongID() +")");
			for(IChannel chan : guild.getChannels()) {
				Log.print("\t\tChannel: " + chan.getName() + " (ID: " + chan.getLongID() + ")");
			}
		}
		Log.print("");
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IGuild guild = event.getGuild();
		IChannel channel = event.getChannel();
		IMessage message = event.getMessage();

		//Check if the bot doesn't answer to itself or to another bot
		if(event.getAuthor().isBot() || event.getAuthor().getStringID().equals(event.getClient().getOurUser().getStringID())) {
			return;
		}

		if(Main.DEBUG && guild.getStringID().equals("331152695006330880") || !Main.DEBUG && channel.getStringID().equals("275615361997471745")) {
			if(TriviaCommand.QUIZZ_STARTED) {
				TriviaCommand.checkAnswer(message);
			}
			else if(message.getContent().startsWith("/")) {
				BotUtils.executeCommand(event);
			}
		}
	}
}