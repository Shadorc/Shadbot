package me.shadorc.discordbot.command.game.trivia;

import java.io.IOException;

import org.json.JSONException;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends AbstractCommand {

	public TriviaCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "trivia");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		TriviaManager triviaManager = TriviaManager.CHANNELS_TRIVIA.get(context.getChannel().getLongID());

		if(triviaManager == null) {
			try {
				triviaManager = new TriviaManager(context);
				triviaManager.start();
				TriviaManager.CHANNELS_TRIVIA.putIfAbsent(context.getChannel().getLongID(), triviaManager);

			} catch (JSONException | IOException err) {
				ExceptionUtils.manageException("getting a question", context, err);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Trivia game has already been started.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Trivia game in which everyone can participate.**")
				.appendField("Gains", "The winner gets **" + TriviaManager.MIN_GAINS + " coins** plus a bonus depending on his speed to answer.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
