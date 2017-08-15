package me.shadorc.discordbot.events;

import java.util.Timer;
import java.util.TimerTask;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.game.TriviaCmd;
import me.shadorc.discordbot.command.game.TriviaCmd.GuildTriviaManager;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
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
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");
		event.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");

		// Update Shadbot stats every 3 hours
		final int period = 1000 * 60 * 60 * 3;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				NetUtils.postStats();
			}
		};
		timer.schedule(timerTask, 0, period);
	}

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		if(event.getAuthor().isBot()) {
			return;
		}

		if(Config.VERSION.isBeta() && event.getChannel().getLongID() != Config.DEBUG_CHANNEL_ID
				|| !Config.VERSION.isBeta() && event.getChannel().getLongID() == Config.DEBUG_CHANNEL_ID) {
			return;
		}

		IMessage message = event.getMessage();
		GuildTriviaManager gtm = TriviaCmd.getGuildTriviaManager(event.getGuild());
		if(gtm != null && gtm.isStarted()) {
			gtm.checkAnswer(message);
		} else if(message.getContent().startsWith(Storage.getSetting(event.getGuild(), Setting.PREFIX).toString())) {
			CommandManager.getInstance().manage(event);
		}
	}

	@EventSubscriber
	public void onGuildCreateEvent(GuildCreateEvent event) {
		LogUtils.info("Shadbot is now connected to guild: " + event.getGuild().getName()
				+ " (ID: " + event.getGuild().getStringID()
				+ " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onGuildLeaveEvent(GuildLeaveEvent event) {
		LogUtils.info("Shadbot has been disconnected from guild: " + event.getGuild().getName()
				+ " (ID: " + event.getGuild().getStringID()
				+ " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onUserVoiceChannelEvent(UserVoiceChannelEvent event) {
		IVoiceChannel botVoiceChannel = event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel();
		if(botVoiceChannel != null) {

			GuildMusicManager gmm = GuildMusicManager.getGuildAudioPlayer(botVoiceChannel.getGuild());

			// TODO: Remove
			if(gmm.getChannel() == null && !Config.VERSION.isBeta()) {
				LogUtils.warn("Somewhere, something very strange happened... Shadbot was in a guild without channel set.");
				gmm.setChannel(event.getGuild().getGeneralChannel());
			}

			if(this.isAlone(botVoiceChannel) && !gmm.isLeavingScheduled()) {
				BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.", gmm.getChannel());
				gmm.getScheduler().setPaused(true);
				gmm.scheduleLeave();

			} else if(!this.isAlone(botVoiceChannel) && gmm.isLeavingScheduled()) {
				BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.", gmm.getChannel());
				gmm.getScheduler().setPaused(false);
				gmm.cancelLeave();
			}
		}
	}

	private boolean isAlone(IVoiceChannel voiceChannel) {
		return voiceChannel.getConnectedUsers().size() == 1;
	}
}