package me.shadorc.discordbot.command.game.hangman;

import java.io.IOException;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class HangmanCmd extends AbstractCommand {

	public HangmanCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "hangman");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		HangmanManager hangmanManager = HangmanManager.CHANNELS_HANGMAN.get(context.getChannel().getLongID());

		if(hangmanManager == null) {
			try {
				hangmanManager = new HangmanManager(context);
				hangmanManager.start();
				HangmanManager.CHANNELS_HANGMAN.putIfAbsent(context.getChannel().getLongID(), hangmanManager);

			} catch (IOException err) {
				ExceptionUtils.manageException("getting a word", context, err);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Hangman game has already been started by **"
					+ hangmanManager.getAuthor().getName() + "**. Please, wait for him to finish.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Hangman game.**")
				.appendField("Gains", "The winner gets **" + HangmanManager.MIN_GAINS + " coins** plus a bonus depending on "
						+ "the number of errors.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
