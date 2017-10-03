package me.shadorc.discordbot.events.music;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceDisconnectedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

@SuppressWarnings("ucd")
public class VoiceChannelListener {

	@EventSubscriber
	public void onVoiceDisconnectedEvent(VoiceDisconnectedEvent event) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(event.getGuild());
		if(musicManager != null) {
			musicManager.delete();
			LogUtils.info("{Guild ID: " + event.getGuild().getLongID() + ")} Voice channel leaved.");
		}
	}

	@EventSubscriber
	public void onUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
		this.check(event.getGuild());
	}

	@EventSubscriber
	public void onUserVoiceChannelLeaveEvent(UserVoiceChannelLeaveEvent event) {
		this.check(event.getGuild());
	}

	@EventSubscriber
	public void onUserVoiceChannelMoveEvent(UserVoiceChannelMoveEvent event) {
		this.check(event.getGuild());
	}

	private void check(IGuild guild) {
		IVoiceChannel botVoiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(botVoiceChannel != null) {

			GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(guild);

			if(musicManager == null) {
				LogUtils.debug(this.getClass(), guild, "MusicManager was null while Shadbot was in a voice channel.");
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
