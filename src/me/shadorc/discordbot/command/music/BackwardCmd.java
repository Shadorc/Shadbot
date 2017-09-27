package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class BackwardCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public BackwardCmd() {
		super(CommandCategory.MUSIC, Role.USER, "backward");
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

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String numStr = context.getArg().trim();
		if(!StringUtils.isPositiveInt(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		try {
			long time = -TimeUnit.SECONDS.toMillis(Integer.parseInt(numStr));
			long newPosition = musicManager.getScheduler().changePosition(time);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " New position: " + StringUtils.formatDuration(newPosition), context.getChannel());
		} catch (IllegalArgumentException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " New position is past the beginning of the song.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Fast backward current song a specified amount of time (in seconds).**")
				.appendField("Usage", "`" + context.getPrefix() + "backward <sec>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
