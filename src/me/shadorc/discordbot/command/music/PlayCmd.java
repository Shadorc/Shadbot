package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.events.AudioLoadResultListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class PlayCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public PlayCmd() {
		super(CommandCategory.MUSIC, Role.USER, "play", "add", "queue");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		IVoiceChannel botVoiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(userVoiceChannel == null) {
			if(botVoiceChannel == null) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Join a voice channel before using this command.", context.getChannel());
			} else {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " I'm currently playing music in voice channel " + botVoiceChannel.mention()
						+ ", join me before using this command.", context.getChannel());
			}
			return;
		}

		if(botVoiceChannel != null && !botVoiceChannel.equals(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " I'm currently playing music in voice channel " + botVoiceChannel.mention()
					+ ", join me before using this command.", context.getChannel());
			return;
		}

		String identifier;
		if(context.getArg().startsWith("soundcloud ")) {
			identifier = AudioLoadResultListener.SC_SEARCH + context.getArg().replace("soundcloud ", "");
		} else if(NetUtils.isValidURL(context.getArg())) {
			identifier = context.getArg();
		} else {
			identifier = AudioLoadResultListener.YT_SEARCH + context.getArg();
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null
				// FIXME: Should we check this ? Creating a new manager is not problematic ?
				|| musicManager.getScheduler().isStopped()) {
			musicManager = GuildMusicManager.createGuildMusicManager(context.getGuild());
		} else if(musicManager.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You've reached the maximum number of tracks in the playlist (Max: "
					+ Config.MAX_PLAYLIST_SIZE + ").", context.getChannel());
			return;
		}

		musicManager.setChannel(context.getChannel());
		musicManager.setDj(context.getAuthor());
		AudioLoadResultListener resultListener = new AudioLoadResultListener(identifier, botVoiceChannel, userVoiceChannel, musicManager);
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier, resultListener);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play the music(s) from the url, search terms or playlist.\nYou can also search on SoundCloud by using /play soundcloud <search>**")
				.appendField("Usage", "`" + context.getPrefix() + "play <url>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
