package me.shadorc.discordbot.events;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

@SuppressWarnings("ucd")
public class VoiceChannelListener {

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

			GuildMusicManager gmm = GuildMusicManager.getGuildMusicManager(guild);

			if(gmm == null) {
				return;
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
		return voiceChannel.getConnectedUsers().stream().filter(user -> !user.isBot()).count() == 0;
	}
}
