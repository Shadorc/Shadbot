package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

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
			BotUtils.send(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		musicManager.getScheduler().clearPlaylist();
		BotUtils.send(Emoji.CHECK_MARK + " Playlist cleared.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Clear current playlist.**");
		BotUtils.send(builder.build(), context.getChannel());
	}

}
