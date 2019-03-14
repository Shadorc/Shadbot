package me.shadorc.shadbot.listener.music;

import java.util.concurrent.atomic.AtomicInteger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.publisher.Mono;

public class TrackEventListener extends AudioEventAdapter {

	private final Snowflake guildId;
	private final AtomicInteger errorCount;

	public TrackEventListener(Snowflake guildId) {
		super();
		this.guildId = guildId;
		this.errorCount = new AtomicInteger(0);
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}

		final String message = String.format(Emoji.MUSICAL_NOTE + " Currently playing: **%s**",
				FormatUtils.trackName(track.getInfo()));
		guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(message, channel))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}

		if(endReason.mayStartNext) {
			this.errorCount.set(0); // Everything seems to be fine, reset error counter.
			this.nextOrEnd()
					.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}

		this.errorCount.incrementAndGet();

		final String errMessage = TextUtils.cleanLavaplayerErr(err);

		LogUtils.info("{Guild ID: %d} %sTrack exception: %s", this.guildId.asLong(), this.errorCount.get() > 3 ? "(Ignored) " : "", errMessage);

		final StringBuilder strBuilder = new StringBuilder();
		if(this.errorCount.get() <= 3) {
			strBuilder.append(String.format(Emoji.RED_CROSS + " Sorry, %s. I'll try to play the next available song.",
					errMessage.toLowerCase()));
		}

		if(this.errorCount.get() == 3) {
			LogUtils.info("{Guild ID: %d} Too many errors in a row. They will be ignored until a music can be played.", this.guildId.asLong());
			strBuilder.append("\n" + Emoji.RED_FLAG + " Too many errors in a row, I will ignore them until I find a music that can be played.");
		}

		guildMusic.getMessageChannel()
				.filter(ignored -> strBuilder.length() > 0)
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.then(this.nextOrEnd())
				.subscribe(null, thr -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), thr));
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}

		LogUtils.info("{Guild ID: %d} Music stuck, skipping it.", this.guildId.asLong());

		guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll try to play the next available song.",
						channel))
				.then(this.nextOrEnd())
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
	}

	private Mono<Void> nextOrEnd() {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return Mono.empty();
		}

		// If the next track could be started
		if(guildMusic.getTrackScheduler().nextTrack()) {
			return Mono.empty();
		}
		return guildMusic.end();
	}
}
