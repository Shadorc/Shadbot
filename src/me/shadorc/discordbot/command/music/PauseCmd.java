package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class PauseCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public PauseCmd() {
		super(CommandCategory.MUSIC, Role.USER, "pause", "unpause", "resume");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		AudioPlayer audioPlayer = musicManager.getScheduler().getAudioPlayer();
		audioPlayer.setPaused(!audioPlayer.isPaused());
		if(audioPlayer.isPaused()) {
			BotUtils.sendMessage(Emoji.PAUSE + " Music paused by " + context.getAuthorName() + ".", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Music resumed by " + context.getAuthorName() + ".", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Pause current music. Use this command again to resume.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}