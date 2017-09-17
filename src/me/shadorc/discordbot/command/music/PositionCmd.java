package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class PositionCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public PositionCmd() {
		super(Role.USER, "forward", "backward");
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
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		int time = (context.getCommand().equals("forward") ? 1 : -1) * Integer.parseInt(numStr) * 1000;

		try {
			musicManager.getScheduler().changePosition(time);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " New position: " + StringUtils.formatDuration(musicManager.getScheduler().getPosition()), context.getChannel());
		} catch (IllegalArgumentException err) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " New position is negative or superior to the music duration.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Fast forward/fast backward current song a specified amount of time (in seconds).**")
				.appendField("Usage", "`" + context.getPrefix() + "forward <sec>`"
						+ "\n`" + context.getPrefix() + "backward <sec>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
