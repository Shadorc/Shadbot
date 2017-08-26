package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class SkipCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public SkipCmd() {
		super(Role.USER, "skip", "next");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :) You can use " + context.getPrefix() + "skip <num> to skip directly to a music in the playlist.", context);
			}
			return;
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		if(context.hasArg()) {
			String numStr = context.getArg();
			if(!StringUtils.isPositiveInteger(numStr)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Number must be between 1 and " + musicManager.getScheduler().getPlaylist().size() + ".", context.getChannel());
				return;
			}

			int num = Integer.parseInt(numStr);
			if(num < 1 || num > musicManager.getScheduler().getPlaylist().size()) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Number must be between 1 and " + musicManager.getScheduler().getPlaylist().size() + ".", context.getChannel());
				return;
			}
			musicManager.getScheduler().skipTo(num);
			return;
		}

		if(!musicManager.getScheduler().nextTrack()) {
			musicManager.end();
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Skip the current music and play the next one if it exists. "
						+ "You can also directly skip to a music in the playlist by specifying its number.**")
				.appendField("Usage", context.getPrefix() + "skip"
						+ "\n" + context.getPrefix() + "skip <num>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}