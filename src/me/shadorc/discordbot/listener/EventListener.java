package me.shadorc.discordbot.listener;

import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.game.TriviaCmd.GuildTriviaManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

public class EventListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		Log.info("------------------- Shadbot is connected -------------------");
		event.getClient().changePlayingText("/help");
	}

	@SuppressWarnings("unused")
	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage message = event.getMessage();

		if(event.getAuthor().isBot()) {
			return;
		}

		if((Main.IS_BETA && !event.getChannel().getStringID().equals(Main.DEBUG_CHANNEL_ID)) 
				|| (!Main.IS_BETA && event.getChannel().getStringID().equals(Main.DEBUG_CHANNEL_ID))) {
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

	@EventSubscriber
	public void onGuildCreateEvent(GuildCreateEvent event) {
		Log.info("Shadbot is now connected to guild: " + event.getGuild().getName() + " (ID: " + event.getGuild().getStringID() + ")");
	}
}