package me.shadorc.discordbot.command.utils.poll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class PollManager {

	protected static final ConcurrentHashMap<Long, PollManager> CHANNELS_POLL = new ConcurrentHashMap<>();

	private final Map<String, List<IUser>> choicesMap;
	private final Context context;
	private final int duration;
	private final String question;
	private final ScheduledExecutorService executor;

	private IMessage message;
	private long startTime;
	private ScheduledFuture<?> stopTask;

	protected PollManager(Context context, int duration, String question, List<String> choicesList) {
		this.context = context;
		this.duration = duration;
		this.question = question;
		this.choicesMap = new LinkedHashMap<String, List<IUser>>();
		for(String choice : choicesList) {
			choicesMap.put(choice, new ArrayList<>());
		}
		this.executor = Executors.newSingleThreadScheduledExecutor(Utils.getThreadFactoryNamed("Shadbot-PollManager@" + this.hashCode()));
	}

	protected void start() {
		startTime = System.currentTimeMillis();
		stopTask = executor.schedule(() -> this.stop(), duration, TimeUnit.SECONDS);
		this.show();
	}

	protected void stop() {
		stopTask.cancel(false);
		executor.shutdownNow();
		CHANNELS_POLL.remove(context.getChannel().getLongID());
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
			choicesStr.append("\n\t\t" + FormatUtils.formatList(votersList.subList(0, Math.min(5, votersList.size())), user -> user.getName(), ", "));
			if(votersList.size() > 5) {
				choicesStr.append("...");
			}
			count++;
		}

		EmbedBuilder embed = Utils.getDefaultEmbed()
				.withAuthorName("Poll (Created by: " + context.getAuthorName() + ")")
				.withThumbnail(context.getAuthor().getAvatarURL())
				.appendDescription("Vote using: `" + context.getPrefix() + "poll <choice>`"
						+ "\n\n__**" + question + "**__"
						+ choicesStr.toString())
				.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png")
				.withFooterText(executor.isShutdown() ? "Finished" : "Time left: " + FormatUtils.formatDuration(MathUtils.remainingTime(startTime, TimeUnit.SECONDS.toMillis(duration))));

		this.message = BotUtils.sendMessage(embed.build(), context.getChannel()).get();
	}

	protected IUser getCreator() {
		return context.getAuthor();
	}

	protected int getNumChoices() {
		return new ArrayList<String>(choicesMap.keySet()).size();
	}
}