package me.shadorc.discordbot.command.french;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TwitterUtils;
import sx.blah.discord.util.EmbedBuilder;
import twitter4j.TwitterException;

public class HolidaysCmd extends Command {

	private final RateLimiter rateLimiter;

	public HolidaysCmd() {
		super(false, "vacs", "vacances");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			TwitterUtils.connection();
			String holidays = TwitterUtils.getInstance().getUserTimeline("Vacances_Zone" + context.getArg().toUpperCase()).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				throw new MissingArgumentException();
			} else {
				Log.error("An error occured while getting holidays information.", e, context.getChannel());
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show the number of remaining days before the next school holidays for the indicated zone.**")
				.appendField("Usage", context.getPrefix() + "vacs <A|B|C>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
