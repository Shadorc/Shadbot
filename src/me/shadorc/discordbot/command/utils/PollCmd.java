package me.shadorc.discordbot.command.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.Timer;

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
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class PollCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Long, PollManager> CHANNELS_POLL = new ConcurrentHashMap<>();

	private static final int MIN_CHOICES_NUM = 2;
	private static final int MAX_CHOICES_NUM = 10;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 3600;

	public PollCmd() {
		super(CommandCategory.UTILS, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "poll");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		PollManager pollManager = CHANNELS_POLL.get(context.getChannel().getLongID());

		if(pollManager == null) {
			this.createPoll(context);

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

	private void createPoll(Context context) throws MissingArgumentException {
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
		CHANNELS_POLL.putIfAbsent(context.getChannel().getLongID(), pollManager);
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

	protected class PollManager {

		private final Map<String, List<IUser>> choicesMap;
		private final Context context;
		private final String question;
		private final Timer timer;

		private IMessage message;
		private long startTime;

		protected PollManager(Context context, int duration, String question, List<String> choicesList) {
			this.context = context;
			this.question = question;
			this.choicesMap = new LinkedHashMap<String, List<IUser>>();
			for(String choice : choicesList) {
				choicesMap.put(choice, new ArrayList<>());
			}
			this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(duration), event -> {
				this.stop();
			});
		}

		protected void start() {
			startTime = System.currentTimeMillis();
			timer.start();
			this.show();
		}

		protected void stop() {
			CHANNELS_POLL.remove(context.getChannel().getLongID());
			timer.stop();
			this.show();
		}

		protected synchronized void vote(IUser user, int num) {
			List<String> choicesList = new ArrayList<String>(choicesMap.keySet());
			for(String choice : choicesList) {
				if(choicesMap.get(choice).remove(user)) {
					break;
				}
			}

			String choice = choicesList.get(num - 1);
			List<IUser> usersList = choicesMap.get(choice);
			usersList.add(user);
			choicesMap.put(choice, usersList);
			this.show();
		}

		protected void show() {
			BotUtils.deleteIfPossible(context.getChannel(), message);

			int count = 1;
			StringBuilder choicesStr = new StringBuilder();

			for(String choice : choicesMap.keySet()) {
				List<IUser> votersList = choicesMap.get(choice);
				String vote = votersList.isEmpty() ? "" : " *(Vote: " + votersList.size() + ")*";
				choicesStr.append("\n\t**" + count + ".** " + choice + vote);
				choicesStr.append("\n\t\t" + StringUtils.formatList(votersList.subList(0, Math.min(5, votersList.size())), user -> user.getName(), ", "));
				if(votersList.size() > 5) {
					choicesStr.append("...");
				}
				count++;
			}

			long remainingTime = (timer.getDelay() - (System.currentTimeMillis() - startTime));
			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Poll (Created by: " + context.getAuthorName() + ")")
					.withThumbnail(context.getAuthor().getAvatarURL())
					.appendDescription("Vote using: `" + context.getPrefix() + PollCmd.this.getFirstName() + " <choice>`"
							+ "\n\n__**" + question + "**__"
							+ choicesStr.toString())
					.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png")
					.withFooterText(timer.isRunning() ? ("Time left: " + StringUtils.formatDuration(remainingTime)) : "Finished");

			this.message = BotUtils.sendMessage(embed.build(), context.getChannel()).get();
		}

		protected IUser getCreator() {
			return context.getAuthor();
		}

		protected int getNumChoices() {
			return new ArrayList<String>(choicesMap.keySet()).size();
		}
	}
}
