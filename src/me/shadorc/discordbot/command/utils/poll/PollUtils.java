package me.shadorc.discordbot.command.utils.poll;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.command.Emoji;

public class PollUtils {

	private static final int MIN_CHOICES_NUM = 2;
	private static final int MAX_CHOICES_NUM = 10;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 3600;

	public static void createPoll(Context context) throws MissingArgumentException {
		String[] splitArgs = StringUtils.getSplittedArg(context.getArg(), 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String durationStr = splitArgs[0];
		if(!StringUtils.isIntBetween(durationStr, MIN_DURATION, MAX_DURATION)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid duration, must be between " + MIN_DURATION + "sec and "
					+ MAX_DURATION + "sec.", context.getChannel());
			return;
		}

		List<String> substrings = StringUtils.getQuotedWords(splitArgs[1]);
		if(substrings.isEmpty() || StringUtils.getCharCount(splitArgs[1], '"') % 2 != 0) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Question and choices cannot be empty and must be enclosed in quotation marks.", context.getChannel());
			return;
		}

		// Remove duplicate choices
		List<String> choicesList = new ArrayList<>(substrings.subList(1, substrings.size()).stream().distinct().collect(Collectors.toList()));

		if(choicesList.size() < MIN_CHOICES_NUM || choicesList.size() > MAX_CHOICES_NUM) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You must specify between " + MIN_CHOICES_NUM + " and "
					+ MAX_CHOICES_NUM + " different non-empty choices.", context.getChannel());
			return;
		}

		int duration = Integer.parseInt(durationStr);
		String question = substrings.get(0);

		PollManager pollManager = new PollManager(context, duration, question, choicesList);
		pollManager.start();
		PollManager.CHANNELS_POLL.putIfAbsent(context.getChannel().getLongID(), pollManager);
	}
}
