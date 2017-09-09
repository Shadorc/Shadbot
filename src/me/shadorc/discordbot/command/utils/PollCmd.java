package me.shadorc.discordbot.command.utils;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.swing.Timer;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class PollCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<IChannel, PollManager> CHANNELS_POLL = new ConcurrentHashMap<>();

	private final RateLimiter rateLimiter;

	private static final int MIN_CHOICES_NUM = 2;
	private static final int MAX_CHOICES_NUM = 10;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 3600;

	public PollCmd() {
		super(Role.USER, "poll");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		PollManager pollManager = CHANNELS_POLL.get(context.getChannel());

		if(pollManager == null) {
			if(context.getArg().length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Your message is waaay too long, it must not contain more than "
						+ EmbedBuilder.DESCRIPTION_CONTENT_LIMIT + " characters.", context.getChannel());
				return;
			}

			String[] splitArgs = context.getArg().split(" ", 2);
			if(splitArgs.length != 2) {
				throw new MissingArgumentException();
			}

			String durationStr = splitArgs[0];
			if(!StringUtils.isPositiveInt(durationStr)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid duration.", context.getChannel());
				return;
			}

			int duration = Integer.parseInt(durationStr);
			if(duration < MIN_DURATION || duration > MAX_DURATION) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Duration must be between " + MIN_DURATION + "sec and "
						+ MAX_DURATION + "sec.", context.getChannel());
				return;
			}

			if(StringUtils.getCharCount(splitArgs[1], '"') % 2 != 0) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " You forgot a quotation mark.", context.getChannel());
				return;
			}

			List<String> substrings = StringUtils.getQuotedWords(splitArgs[1]);

			String question = substrings.get(0);
			if(question.isEmpty()) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " The question can not be empty.", context.getChannel());
				return;
			}

			// Remove duplicate choices
			List<String> choicesList = new ArrayList<>(substrings.subList(1, substrings.size()).stream().distinct().collect(Collectors.toList()));
			// Remove empty choices
			choicesList.removeAll(Collections.singleton(""));

			if(choicesList.size() < MIN_CHOICES_NUM || choicesList.size() > MAX_CHOICES_NUM) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " You must specify between " + MIN_CHOICES_NUM + " and "
						+ MAX_CHOICES_NUM + " different non-empty choices.", context.getChannel());
				return;
			}

			pollManager = new PollManager(context.getChannel(), context.getAuthor(), duration, question, choicesList);
			pollManager.start();
			CHANNELS_POLL.putIfAbsent(context.getChannel(), pollManager);
			return;
		}

		if(context.getArg().equals("stop") || context.getArg().equals("cancel")) {
			if(context.getAuthor().equals(pollManager.getCreator()) || context.getAuthorRole().equals(Role.ADMIN)) {
				pollManager.stop();
			}
			return;
		}

		String numStr = context.getArg();
		if(!StringUtils.isPositiveInt(numStr)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);
		if(num < 1 || num > pollManager.getNumChoices()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid num, must be between 1 and " + pollManager.getNumChoices() + ".", context.getChannel());
			return;
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		pollManager.vote(context.getAuthor(), num);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Create a poll.**")
				.appendField("Usage", "**Create a poll:** " + context.getPrefix() + "poll <duration> \"question\" \"choice1\" \"choice2\"..."
						+ "\n**Vote:** " + context.getPrefix() + "poll <choice>"
						+ "\n**Stop (author/admin):** " + context.getPrefix() + "poll stop", false)
				.appendField("Restrictions", "**duration** - in seconds, must be between 10s and 3600s (1 hour)"
						+ "\n**question and choices** - in quotation marks"
						+ "\n**choices** - min: 2, max: 10", false)
				.appendField("Example", context.getPrefix() + "poll 120 \"Where do we eat at noon?\" \"White\" \"53\" \"A dog\"", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	protected class PollManager {

		private final IChannel channel;
		private final IUser creator;
		private final String question;
		private final Map<String, List<IUser>> choicesMap;
		private final Timer timer;

		private IMessage message;
		private long startTime;

		protected PollManager(IChannel channel, IUser creator, int duration, String question, List<String> choicesList) {
			this.channel = channel;
			this.creator = creator;
			this.question = question;
			this.choicesMap = new LinkedHashMap<String, List<IUser>>();
			for(String choice : choicesList) {
				choicesMap.put(choice, new ArrayList<>());
			}
			this.timer = new Timer(duration * 1000, event -> {
				this.stop();
			});
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

		protected void start() {
			startTime = System.currentTimeMillis();
			timer.start();
			this.show();
		}

		protected void stop() {
			timer.stop();
			this.show();
			CHANNELS_POLL.remove(channel);
		}

		protected void show() {
			if(message != null && BotUtils.hasPermission(channel, Permissions.MANAGE_MESSAGES)) {
				message.delete();
			}

			List<String> choicesList = new ArrayList<String>(choicesMap.keySet());

			StringBuilder choicesStr = new StringBuilder();
			for(int i = 0; i < choicesList.size(); i++) {
				List<IUser> usersList = choicesMap.get(choicesList.get(i));
				String vote = usersList.isEmpty() ? "" : " *(Vote: " + usersList.size() + ")*";
				choicesStr.append("\n\t**" + (i + 1) + ".** " + choicesList.get(i) + vote);
				if(!usersList.isEmpty()) {
					choicesStr.append("\n\t\t");
					if(usersList.size() > 5) {
						choicesStr.append(StringUtils.formatList(usersList.subList(0, 5), user -> user.getName(), ", ") + "...");
					} else {
						choicesStr.append(StringUtils.formatList(usersList, user -> user.getName(), ", "));
					}
				}
			}

			long remainingTime = (timer.getDelay() - (System.currentTimeMillis() - startTime));
			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Poll (Created by: " + creator.getName() + ")")
					.withThumbnail(creator.getAvatarURL())
					.appendDescription("Vote using: " + Storage.getSetting(channel.getGuild(), Setting.PREFIX) + "poll <choice>"
							+ "\n\n__" + question + "__"
							+ choicesStr.toString())
					.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png")
					.withFooterText(timer.isRunning() ? ("Time left: " + StringUtils.formatDuration(remainingTime)) : "Finished");
			this.sendPoll(embed.build());
		}

		protected IUser getCreator() {
			return creator;
		}

		protected int getNumChoices() {
			return new ArrayList<String>(choicesMap.keySet()).size();
		}

		// FIXME: Use BotUtils and not this ugly hack
		private void sendPoll(EmbedObject embed) {
			if(!ShardListener.isShardConnected(channel.getShard())) {
				return;
			}

			if(!BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot send embed links due to the lack of permission."
						+ "\nPlease, check my permissions and channel-specific ones to verify that **Send Embed links** is checked.", channel);
				LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + ")} Shadbot wasn't allowed to send embed link.");
				return;
			}

			RequestBuffer.request(() -> {
				try {
					message = channel.sendMessage(embed);
				} catch (MissingPermissionsException err) {
					LogUtils.error("{Guild ID: " + channel.getGuild().getLongID() + ")} Missing permissions.", err);
				} catch (DiscordException err) {
					LogUtils.error("Discord exception while sending embed link: " + err.getErrorMessage(), err);
				}
			});
		}
	}
}
