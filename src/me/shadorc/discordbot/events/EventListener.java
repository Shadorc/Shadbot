package me.shadorc.discordbot.events;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.game.TriviaCmd.GuildTriviaManager;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class EventListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		Log.info("------------------- Shadbot is ready [BETA:" + Config.VERSION.isBeta() + "] -------------------");
		event.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage message = event.getMessage();

		if(event.getAuthor().isBot()) {
			return;
		}

		if((Config.VERSION.isBeta() && event.getChannel().getLongID() != Config.DEBUG_CHANNEL_ID)
				|| (!Config.VERSION.isBeta() && event.getChannel().getLongID() == Config.DEBUG_CHANNEL_ID)) {
			return;
		}

		GuildTriviaManager gtm = TriviaCmd.getGuildTriviaManager(event.getGuild());
		if(gtm != null && gtm.isStarted()) {
			gtm.checkAnswer(message);
		} else if(message.getContent().startsWith(Storage.getSetting(event.getGuild(), Setting.PREFIX).toString())) {
			CommandManager.getInstance().manage(event);
		}
	}

	@EventSubscriber
	public void onGuildCreateEvent(GuildCreateEvent event) {
		Log.info("Shadbot is now connected to guild: " + event.getGuild().getName()
				+ " (ID: " + event.getGuild().getStringID()
				+ " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onGuildLeaveEvent(GuildLeaveEvent event) {
		Log.info("Shadbot has been kicked from: " + event.getGuild().getName()
				+ " (ID: " + event.getGuild().getStringID()
				+ " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onUserVoiceChannelEvent(UserVoiceChannelEvent event) {
		IVoiceChannel botVoiceChannel = event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel();
		if(botVoiceChannel != null) {
			GuildMusicManager gmm = GuildMusicManager.getGuildAudioPlayer(botVoiceChannel.getGuild());
			if(this.isAlone(botVoiceChannel) && !gmm.isCancelling()) {
				BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.", gmm.getChannel());
				gmm.getScheduler().setPaused(true);
				gmm.scheduleLeave();
			} else if(!this.isAlone(botVoiceChannel) && gmm.isCancelling()) {
				BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.", gmm.getChannel());
				gmm.getScheduler().setPaused(false);
				gmm.cancelLeave();
			}
		}
	}

	private boolean isAlone(IVoiceChannel voiceChannel) {
		return voiceChannel.getConnectedUsers().size() <= 1;
	}
}