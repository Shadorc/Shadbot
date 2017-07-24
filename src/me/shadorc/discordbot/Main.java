package me.shadorc.discordbot;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.discordbot.listener.ChannelListener;
import me.shadorc.discordbot.listener.EventListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class Main {

	private static IDiscordClient client;

	public static void main(String[] args) {
		client = new ClientBuilder()
				.withToken(Storage.get(API_KEYS.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new EventListener());
		client.getDispatcher().registerListener(new ChannelListener());

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
		AudioSourceManagers.registerLocalSource(GuildMusicManager.PLAYER_MANAGER);
	}

	public static IDiscordClient getClient() {
		return client;
	}

	/*
	 * //	public void set_chatbot() {
//		if(message.getAuthor().getName().equals("Shadorc")) {
//			if(arg != null) {
//				if(arg.equalsIgnoreCase(ChatBot.ALICE.toString())) {
//					Chat.setChatbot(ChatBot.ALICE);
//				} else if(arg.equalsIgnoreCase(ChatBot.CLEVERBOT.toString())) {
//					Chat.setChatbot(ChatBot.CLEVERBOT);
//				}
//				BotUtils.sendMessage("ChatBot has been set to " + arg.toUpperCase(), channel);
//			}
//		}
//	}
	 * */

}