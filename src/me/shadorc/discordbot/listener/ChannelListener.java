package me.shadorc.discordbot.listener;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelEvent;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class ChannelListener {

	@EventSubscriber
	public void onUserVoiceChannelEvent(UserVoiceChannelEvent event) {
		IVoiceChannel botVoiceChannel = event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel();
		if(botVoiceChannel != null) {
			GuildMusicManager gmm = GuildMusicManager.getGuildAudioPlayer(botVoiceChannel.getGuild());
			if(this.isAlone(botVoiceChannel) && !gmm.isCancelling()) {
				BotUtils.sendMessage(Emoji.INFO + " Il n'y a plus personne qui écoute de la musique, musique mis en pause, je quitterai le salon dans 1 minute.", gmm.getRequestedChannel());
				gmm.getScheduler().setPaused(true);
				gmm.scheduleLeave();
			} else if(!this.isAlone(botVoiceChannel) && gmm.isCancelling()){
				BotUtils.sendMessage(Emoji.INFO + " Quelqu'un m'a rejoint, reprise de la musique.", gmm.getRequestedChannel());
				gmm.getScheduler().setPaused(false);
				gmm.cancelLeave();
			}
		}
	}

	private boolean isAlone(IVoiceChannel voiceChannel) {
		return voiceChannel.getConnectedUsers().size() <= 1;
	}
}
