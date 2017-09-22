package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class ClearCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public ClearCmd() {
		super(CommandCategory.MUSIC, Role.USER, "clear");
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

		musicManager.getScheduler().clearPlaylist();
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Playlist cleared.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Clear current playlist.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
