package me.shadorc.discordbot.command.utils.poll;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class PollCmd extends AbstractCommand {

	public PollCmd() {
		super(CommandCategory.UTILS, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "poll");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		PollManager pollManager = PollManager.CHANNELS_POLL.get(context.getChannel().getLongID());

		if(pollManager == null) {
			PollUtils.createPoll(context);

		} else if(context.getArg().matches("stop|cancel")
				&& (context.getAuthor().equals(pollManager.getCreator()) || context.getAuthorRole().equals(Role.ADMIN))) {
			pollManager.stop();

		} else {
			String numStr = context.getArg();
			if(!StringUtils.isIntBetween(numStr, 1, pollManager.getNumChoices())) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and " + pollManager.getNumChoices() + ".", context.getChannel());
				return;
			}

			pollManager.vote(context.getAuthor(), Integer.parseInt(numStr));
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Create a poll.**")
				.appendField("Usage", "**Create a poll:** `" + context.getPrefix() + this.getFirstName() + " <duration> \"question\" \"choice1\" \"choice2\"...`"
						+ "\n**Vote:** `" + context.getPrefix() + this.getFirstName() + " <choice>`"
						+ "\n**Stop (author/admin):** `" + context.getPrefix() + this.getFirstName() + " stop`", false)
				.appendField("Restrictions", "**duration** - in seconds, must be between 10s and 3600s (1 hour)"
						+ "\n**question and choices** - in quotation marks"
						+ "\n**choices** - min: 2, max: 10", false)
				.appendField("Example", "`" + context.getPrefix() + this.getFirstName() + " 120 \"Where do we eat at noon?\" \"White\" \"53\" \"A dog\"`", false);

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
