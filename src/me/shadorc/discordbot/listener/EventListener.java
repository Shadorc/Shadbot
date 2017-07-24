package me.shadorc.discordbot.listener;

import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.game.TriviaCmd.GuildTriviaManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class EventListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		Log.info("---------------- Shadbot is connected ----------------");
		for(IGuild guild : event.getClient().getGuilds()) {
			Log.info("Shadbot is connected to guild: " + guild.getName() + " (ID: " + guild.getLongID() +")");
		}
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage message = event.getMessage();

		//Check if the bot doesn't answer to itself or to another bot
		if(event.getAuthor().isBot() || event.getAuthor().getStringID().equals(event.getClient().getOurUser().getStringID())) {
			return;
		}

		GuildTriviaManager gtm = TriviaCmd.getGuildTriviaManager(event.getGuild());
		if(gtm != null && gtm.isStarted()) {
			gtm.checkAnswer(message);
		}
		else if(message.getContent().startsWith("/")) {
			BotUtils.executeCommand(event);
		}
	}
}