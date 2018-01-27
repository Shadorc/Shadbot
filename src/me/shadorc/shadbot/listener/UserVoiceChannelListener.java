package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class UserVoiceChannelListener {

	@EventSubscriber
	public void onUserVoiceChannelEvent(UserVoiceChannelEvent event) {
		ShardManager.execute(event.getGuild(), () -> {
			if(event instanceof UserVoiceChannelJoinEvent) {
				this.onUserVoiceChannelJoinEvent((UserVoiceChannelJoinEvent) event);
			} else if(event instanceof UserVoiceChannelLeaveEvent) {
				this.onUserVoiceChannelLeaveEvent((UserVoiceChannelLeaveEvent) event);
			} else if(event instanceof UserVoiceChannelMoveEvent) {
				this.onUserVoiceChannelMoveEvent((UserVoiceChannelMoveEvent) event);
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
		IVoiceChannel botVoiceChannel = guild.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(botVoiceChannel == null) {
			return;
		}

		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guild.getLongID());
		if(guildMusic == null) {
			return;
		}

		if(this.isAlone(botVoiceChannel) && !guildMusic.isLeavingScheduled()) {
			BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.", guildMusic.getChannel());
			guildMusic.getScheduler().getAudioPlayer().setPaused(true);
			guildMusic.scheduleLeave();

		} else if(!this.isAlone(botVoiceChannel) && guildMusic.isLeavingScheduled()) {
			BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.", guildMusic.getChannel());
			guildMusic.getScheduler().getAudioPlayer().setPaused(false);
			guildMusic.cancelLeave();
		}
	}

	private boolean isAlone(IVoiceChannel voiceChannel) {
		return voiceChannel.getConnectedUsers().stream().filter(user -> !user.isBot()).count() == 0;
	}

}
