package me.shadorc.discordbot.command.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
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

	private static final int MIN_CHOICES_NUM = 2;
	private static final int MAX_CHOICES_NUM = 10;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 3600;

	public PollCmd() {
		super(Role.USER, "poll");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		PollManager pollManager = CHANNELS_POLL.get(context.getChannel());

		if(pollManager == null) {
			String[] splitArgs = context.getArg().split(" ", 2);
			if(splitArgs.length != 2) {
				throw new MissingArgumentException();
			}

			String durationStr = splitArgs[0];
			if(!StringUtils.isPositiveInteger(durationStr)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid duration.", context.getChannel());
				return;
			}

			int duration = Integer.parseInt(durationStr);
			if(duration < MIN_DURATION) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Duration must be at least 10 seconds.", context.getChannel());
				return;
			}

			if(duration > MAX_DURATION) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Duration cannot be higher than 3600 seconds (1 hour).", context.getChannel());
				return;
			}

			if(context.getArg().length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Your message is waaay too long, it must not contain more than "
						+ EmbedBuilder.DESCRIPTION_CONTENT_LIMIT + " characters.", context.getChannel());
				return;
			}

			List<String> substrings = StringUtils.getQuotedWords(splitArgs[1]);

			if(substrings.size() < MIN_CHOICES_NUM + 1) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " You must indicate at least two choices.", context.getChannel());
				return;
			}

			if(substrings.size() > MAX_CHOICES_NUM + 1) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " You cannot have more than 10 choices.", context.getChannel());
				return;
			}

			String question = substrings.get(0);
			List<String> choicesList = new ArrayList<>(substrings.subList(1, substrings.size()));

			pollManager = new PollManager(context.getChannel(), context.getAuthor(), duration, question, choicesList);
			pollManager.start();
			CHANNELS_POLL.put(context.getChannel(), pollManager);
			return;
		}

		if(context.getArg().equals("stop") || context.getArg().equals("cancel")) {
			if(context.getAuthor().equals(pollManager.getCreator()) || context.getAuthorRole().equals(Role.ADMIN)) {
				pollManager.stop();
			}
			return;
		}

		String numStr = context.getArg();
		if(!StringUtils.isInteger(numStr)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);
		if(num < 1 || num > pollManager.getNumChoices()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid num, must be between 1 and " + pollManager.getNumChoices() + ".", context.getChannel());
			return;
		}

		if(!pollManager.vote(context.getAuthor(), num)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " " + context.getAuthorName() + ", you've already voted.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
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
		private final ConcurrentHashMap<String, List<IUser>> choicesMap;
		private final Timer timer;

		private IMessage message;
		private long startTime;

		protected PollManager(IChannel channel, IUser creator, int duration, String question, List<String> choicesList) {
			this.channel = channel;
			this.creator = creator;
			this.question = question;
			this.choicesMap = new ConcurrentHashMap<>();
			for(String choice : choicesList) {
				choicesMap.put(choice, new ArrayList<>());
			}
			this.timer = new Timer(duration * 1000, event -> {
				this.stop();
			});
		}

		protected boolean vote(IUser user, int num) {
			List<String> choicesList = new ArrayList<String>(choicesMap.keySet());
			for(String choice : choicesList) {
				if(choicesMap.get(choice).contains(user)) {
					choicesMap.get(choice).remove(user);
					break;
				}
			}

			String choice = choicesList.get(num - 1);
			List<IUser> usersList = choicesMap.get(choice);
			usersList.add(user);
			choicesMap.put(choice, usersList);
			this.show();

			return true;
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

			int remainingTime = (int) ((timer.getDelay() - (System.currentTimeMillis() - startTime)) / 1000);
			EmbedBuilder embed = new EmbedBuilder()
					.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
					.withAuthorName("Poll (Created by: " + creator.getName() + ")")
					.withThumbnail(creator.getAvatarURL())
					.withDescription("**" + question + "**\n" + choicesStr.toString())
					.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png")
					.withFooterText("This poll " + (timer.isRunning() ? ("will end in " + remainingTime + " seconds.") : "is finished."));
			this.sendPoll(embed.build());
		}

		protected IUser getCreator() {
			return creator;
		}

		protected int getNumChoices() {
			return new ArrayList<String>(choicesMap.keySet()).size();
		}

		private void sendPoll(EmbedObject embed) {
			if(!ShardListener.isShardConnected(channel.getShard())) {
				return;
			}

			if(!BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot send embed links due to the lack of permission."
						+ "\nPlease, check my permissions and channel-specific ones to verify that **Send Embed links** is checked.", channel);
				LogUtils.info("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
						+ "Shadbot wasn't allowed to send embed link.");
				return;
			}

			RequestBuffer.request(() -> {
				try {
					message = channel.sendMessage(embed);
				} catch (MissingPermissionsException err) {
					LogUtils.error("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
							+ "Missing permissions.", err);
				} catch (DiscordException err) {
					LogUtils.error("Discord exception while sending embed link: " + err.getErrorMessage(), err);
				}
			});
		}
	}
}
