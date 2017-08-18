package me.shadorc.discordbot.events;

import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class AudioLoadResultListener implements AudioLoadResultHandler {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private final String identifier;
	private final IVoiceChannel botVoiceChannel;
	private final IVoiceChannel userVoiceChannel;
	private final GuildMusicManager musicManager;

	public AudioLoadResultListener(String identifier, IVoiceChannel botVoiceChannel, IVoiceChannel userVoiceChannel, GuildMusicManager musicManager) {
		this.identifier = identifier;
		this.botVoiceChannel = botVoiceChannel;
		this.userVoiceChannel = userVoiceChannel;
		this.musicManager = musicManager;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", musicManager.getChannel());
			return;
		}

		if(musicManager.getScheduler().isPlaying()) {
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(track.getInfo()) + "** has been added to the playlist.", musicManager.getChannel());
		}
		musicManager.getScheduler().queue(track);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		if(playlist.getTracks().isEmpty()) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + identifier.replaceAll(YT_SEARCH + "|" + SC_SEARCH, "") + "\"", musicManager.getChannel());
			return;
		}

		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", musicManager.getChannel());
			return;
		}

		List<AudioTrack> tracks = playlist.getTracks();

		if(identifier.startsWith(YT_SEARCH) || identifier.startsWith(SC_SEARCH)) {
			if(musicManager.getScheduler().isPlaying()) {
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(tracks.get(0).getInfo()) + "** has been added to the playlist.", musicManager.getChannel());
			}
			musicManager.getScheduler().queue(tracks.get(0));
			return;
		}

		for(int i = 0; i < Math.min(200, tracks.size()); i++) {
			AudioTrack track = tracks.get(i);
			musicManager.getScheduler().queue(track);
		}
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " " + musicManager.getScheduler().getPlaylist().size() + " musics have been added to the playlist.", musicManager.getChannel());
	}

	@Override
	public void noMatches() {
		BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + identifier.replaceAll(YT_SEARCH + "|" + SC_SEARCH, "") + "\"", musicManager.getChannel());
	}

	@Override
	public void loadFailed(FriendlyException err) {
		if(err.severity.equals(FriendlyException.Severity.FAULT)) {
			LogUtils.warn("Error while playing music (" + err.getMessage() + "), Shadbot might be able to continue playing.");
		} else {
			BotUtils.sendMessage(Emoji.GEAR + " Sorry, " + err.getMessage().toLowerCase(), musicManager.getChannel());
			LogUtils.warn("Load failed: " + err.getMessage());
		}
	}
}
