package me.shadorc.discordbot.command.music;

import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class PlayCmd extends AbstractCommand {

	protected static final String YT_SEARCH = "ytsearch: ";

	public PlayCmd() {
		super(false, "play", "add");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		IVoiceChannel botVoiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(userVoiceChannel == null) {
			if(botVoiceChannel == null) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Join a vocal channel before using this command.", context.getChannel());
			} else {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Shadbot is currently playing music in voice channel " + botVoiceChannel.mention()
				+ ", join him before using this command.", context.getChannel());
			}
			return;
		}

		if(botVoiceChannel != null && !botVoiceChannel.equals(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Shadbot is currently playing music in voice channel " + botVoiceChannel.mention()
					+ ", join him before using this command.", context.getChannel());
			return;
		}

		final StringBuilder identifier = new StringBuilder();
		if(!NetUtils.isValidURL(context.getArg())) {
			identifier.append(YT_SEARCH);
		}
		identifier.append(context.getArg());

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		musicManager.setChannel(context.getChannel());
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier.toString(), new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel, context.getChannel())) {
					BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", context.getChannel());
					return;
				}

				if(musicManager.getScheduler().isPlaying()) {
					BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(track.getInfo()) + "** has been added to the playlist.", context.getChannel());
				}
				musicManager.getScheduler().queue(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel, context.getChannel())) {
					BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", context.getChannel());
					return;
				}

				List<AudioTrack> tracks = playlist.getTracks();

				if(identifier.toString().startsWith(YT_SEARCH)) {
					if(musicManager.getScheduler().isPlaying()) {
						BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(tracks.get(0).getInfo()) + "** has been added to the playlist.", context.getChannel());
					}
					musicManager.getScheduler().queue(tracks.get(0));
					return;
				}

				for(int i = 0; i < Math.min(200, tracks.size()); i++) {
					AudioTrack track = tracks.get(i);
					musicManager.getScheduler().queue(track);
				}
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " " + musicManager.getScheduler().getPlaylist().size() + " musics have been added to the playlist.", context.getChannel());
			}

			@Override
			public void noMatches() {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " No result for \"" + identifier.toString().replace(YT_SEARCH, "") + "\"", context.getChannel());
			}

			@Override
			public void loadFailed(FriendlyException err) {
				if(err.severity.equals(FriendlyException.Severity.FAULT)) {
					LogUtils.warn("Error while playing music (" + err.getMessage() + "), Shadbot might be able to continue playing.");
				} else {
					BotUtils.sendMessage(Emoji.GEAR + " Sorry, " + err.getMessage().toLowerCase(), context.getChannel());
					LogUtils.warn("Load failed: " + err.getMessage());
				}
			}
		});
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play the music from the url. Search terms or playlist are also possible.**")
				.appendField("Usage", context.getPrefix() + "play <url>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
