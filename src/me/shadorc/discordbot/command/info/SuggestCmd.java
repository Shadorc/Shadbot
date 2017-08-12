package me.shadorc.discordbot.command.info;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

public class SuggestCmd extends Command {

	private final RateLimiter rateLimiter;

	public SuggestCmd() {
		super(false, "suggest");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			BotUtils.sendMessage(Emoji.WARNING + " You can send a suggestion only once every " + rateLimiter.getTimeout() + " seconds.", context.getChannel());
			return;
		}

		RequestBuffer.request(() -> {
			context.getClient().getChannelByID(Config.SUGGEST_CHANNEL_ID).sendMessage(
					context.getAuthorName() + " from Guild \"" + context.getGuild().getName() + "\" suggests : " + context.getArg());
		});

		BotUtils.sendMessage(Emoji.CHECK_MARK + " Suggestion has been sent, thank you !", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Send a message to my developer, this can be a suggestion, a bug report, anything.**")
				.appendField("Usage", context.getPrefix() + "suggest <message>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}