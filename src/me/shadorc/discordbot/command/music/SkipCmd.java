package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.stats.StatsEnum;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class SkipCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public SkipCmd() {
		super(CommandCategory.MUSIC, Role.USER, "skip", "next");
		this.rateLimiter = new RateLimiter(RateLimiter.DEFAULT_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context.getChannel(), context.getAuthor())) {
			BotUtils.sendMessage(Emoji.INFO + " You can use `" + context.getPrefix() + "skip <num>` to jump to a music in the playlist.", context.getChannel());
			StatsManager.increment(StatsEnum.LIMITED_COMMAND, context.getCommand());
			return;
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		if(context.hasArg()) {
			String numStr = context.getArg();
			if(!StringUtils.isIntBetween(numStr, 1, musicManager.getScheduler().getPlaylist().size())) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Number must be between 1 and " + musicManager.getScheduler().getPlaylist().size() + ".", context.getChannel());
				return;
			}

			int num = Integer.parseInt(numStr);
			musicManager.getScheduler().skipTo(num);
			return;
		}

		if(!musicManager.getScheduler().nextTrack()) {
			musicManager.end();
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Skip current music and play the next one if it exists."
						+ "\nYou can also directly skip to a music in the playlist by specifying its number.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<num>]`", false)
				.appendField("Argument", "**num** - [OPTIONAL] the number in the playlist of the music to play", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}