package me.shadorc.discordbot;

import java.util.List;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class AnnotationListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		System.out.println("\n---------------- Shadbot is connected ----------------");
		System.out.println("ShadBot is connected to :");
		List <IGuild> guilds = event.getClient().getGuilds();
		for(IGuild guild : guilds) {
			System.out.println("\t*Guild : " + guild.getName() + " | ID : " + guild.getLongID());
			for(IChannel chan : guild.getChannels()) {
				System.out.println("\t\tChannel : " + chan.getName() + " | ID : " + chan.getLongID());
			}
		}
		System.out.println();
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IGuild guild = event.getGuild();
		IChannel channel = event.getChannel();
		IMessage message = event.getMessage();

		if(Main.DEBUG && guild.getStringID().equals("331152695006330880") || !Main.DEBUG && channel.getStringID().equals("275615361997471745")) {
			if(Trivia.QUIZZ_STARTED) {
				Trivia.checkAnswer(message);
			}
			else if(message.getContent().startsWith("/")) {
				Bot.executeCommand(message, channel);
			}
		}
	}
}