package me.shadorc.discordbot.listener;

import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
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
		IChannel channel = event.getChannel();
		IMessage message = event.getMessage();
		IGuild guild = event.getGuild();

		//Check if the bot doesn't answer to itself or to another bot
		if(event.getAuthor().isBot() || event.getAuthor().getStringID().equals(event.getClient().getOurUser().getStringID())) {
			return;
		}

		if(guild.getStringID().equals("331152695006330880") || channel.getStringID().equals("275615361997471745")) {
			if(TriviaCmd.QUIZZ_STARTED) {
				TriviaCmd.checkAnswer(message);
			}
			else if(message.getContent().startsWith("/")) {
				BotUtils.executeCommand(event);
			}
		}
	}
}