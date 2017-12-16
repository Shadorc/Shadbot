package me.shadorc.discordbot.events;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

@SuppressWarnings("ucd")
public class UserVoiceChannelListener {

	@EventSubscriber
	public void onUserVoiceChannelEvent(UserVoiceChannelEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			long startTime = System.currentTimeMillis();
			if(event instanceof UserVoiceChannelJoinEvent) {
				this.onUserVoiceChannelJoinEvent((UserVoiceChannelJoinEvent) event);
			} else if(event instanceof UserVoiceChannelLeaveEvent) {
				this.onUserVoiceChannelLeaveEvent((UserVoiceChannelLeaveEvent) event);
			} else if(event instanceof UserVoiceChannelMoveEvent) {
				this.onUserVoiceChannelMoveEvent((UserVoiceChannelMoveEvent) event);
			}
			float elapsedSec = (System.currentTimeMillis() - startTime) / 1000f;
			if(elapsedSec > 10) {
				LogUtils.warn("{DEBUG} " + event.getClass().getSimpleName() + " | Long event detected ! "
						+ "Duration: " + String.format("%.1f", elapsedSec) + "s.");
			}
		});
	}

	private void onUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
		this.check(event.getGuild());
	}

	private void onUserVoiceChannelLeaveEvent(UserVoiceChannelLeaveEvent event) {
		this.check(event.getGuild());
	}

	private void onUserVoiceChannelMoveEvent(UserVoiceChannelMoveEvent event) {
		this.check(event.getGuild());
	}

	private synchronized void check(IGuild guild) {
		IVoiceChannel botVoiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(botVoiceChannel != null) {

			GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(guild);

			if(musicManager == null) {
				return;
			}

			if(this.isAlone(botVoiceChannel) && !musicManager.isLeavingScheduled()) {
				BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.", musicManager.getChannel());
				musicManager.getScheduler().getAudioPlayer().setPaused(true);
				musicManager.scheduleLeave();

			} else if(!this.isAlone(botVoiceChannel) && musicManager.isLeavingScheduled()) {
				BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.", musicManager.getChannel());
				musicManager.getScheduler().getAudioPlayer().setPaused(false);
				musicManager.cancelLeave();
			}
		}
	}

	private boolean isAlone(IVoiceChannel voiceChannel) {
		return voiceChannel.getConnectedUsers().stream().filter(user -> !user.isBot()).count() == 0;
	}

}
