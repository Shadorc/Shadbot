package me.shadorc.shadbot.command.utils.poll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.message.UpdateableMessage;

public class PollManager extends AbstractGameManager {

	private final Map<String, List<Snowflake>> choicesMap;

	private final int duration;
	private final String question;
	private final UpdateableMessage message;

	private long startTime;

	public PollManager(AbstractCommand cmd, String prefix, Snowflake guildId, Snowflake channelId, Snowflake authorId, int duration, String question, List<String> choicesList) {
		super(cmd, prefix, guildId, channelId, authorId);
		this.duration = duration;
		this.question = question;
		this.message = new UpdateableMessage(channelId);

		this.choicesMap = new LinkedHashMap<>(10);
		choicesList.stream().forEach(choice -> choicesMap.put(choice, new ArrayList<>()));
	}

	@Override
	public void start() {
		this.schedule(() -> this.stop(), duration, TimeUnit.SECONDS);
		startTime = System.currentTimeMillis();
		this.show();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		PollCmd.MANAGER.remove(this.getChannelId());
		choicesMap.clear();
		this.show();
	}

	protected synchronized void vote(Snowflake userId, int num) {
		choicesMap.values().stream().forEach(list -> list.remove(userId));
		choicesMap.get(new ArrayList<>(choicesMap.keySet()).get(num - 1)).add(userId);
		this.show();
	}

	protected void show() {
		int count = 1;
		StringBuilder choicesStr = new StringBuilder();
		for(String choice : choicesMap.keySet()) {
			List<Snowflake> votersList = choicesMap.get(choice);
			choicesStr.append(String.format("%n\t**%d.** %s", count, choice));
			if(!votersList.isEmpty()) {
				choicesStr.append(String.format(" *(Vote: %d)*", votersList.size()));
				choicesStr.append(String.format("%n\t\t%s", StringUtils.truncate(FormatUtils.format(votersList, IUser::getName, ", "), 30)));
			}
			count++;
		}

		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName(String.format("Poll (Created by: %s)", this.getAuthor().getName()))
				.withThumbnail(this.getAuthor().getAvatarURL())
				.appendDescription(String.format("Vote using: `%s%s <choice>`%n%n__**%s**__%s",
						this.getPrefix(), this.getCmdName(), question, choicesStr.toString()))
				.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png");

		if(this.isTaskDone()) {
			embed.withFooterText("Finished");
		} else {
			embed.withFooterText(String.format("Time left: %s",
					FormatUtils.formatShortDuration(TimeUnit.SECONDS.toMillis(duration) - TimeUtils.getMillisUntil(startTime))));
		}

		RequestFuture<IMessage> messageRequest = message.send(embed.build());
		if(messageRequest != null) {
			messageRequest.get();
		}
	}

	public int getChoicesCount() {
		return choicesMap.size();
	}
}