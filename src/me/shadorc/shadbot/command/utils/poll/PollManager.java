package me.shadorc.shadbot.command.utils.poll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.UpdateableMessage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class PollManager extends AbstractGameManager {

	private final Map<String, List<IUser>> choicesMap;

	private final int duration;
	private final String question;
	private final UpdateableMessage message;

	private long startTime;

	public PollManager(AbstractCommand cmd, String prefix, IChannel channel, IUser author, int duration, String question, List<String> choicesList) {
		super(cmd, prefix, channel, author);
		this.duration = duration;
		this.question = question;
		this.message = new UpdateableMessage(channel);

		this.choicesMap = new LinkedHashMap<String, List<IUser>>(10);
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
		PollCmd.MANAGER.remove(this.getChannel().getLongID());
		choicesMap.clear();
		this.show();
	}

	protected synchronized void vote(IUser user, int num) {
		choicesMap.values().stream().forEach(list -> list.remove(user));
		choicesMap.get(new ArrayList<>(choicesMap.keySet()).get(num - 1)).add(user);
		this.show();
	}

	protected void show() {
		int count = 1;
		StringBuilder choicesStr = new StringBuilder();
		for(String choice : choicesMap.keySet()) {
			List<IUser> votersList = choicesMap.get(choice);
			choicesStr.append(String.format("%n\t**%d.** %s", count, choice));
			if(!votersList.isEmpty()) {
				choicesStr.append(String.format(" *(Vote: %d)*", votersList.size()));
				choicesStr.append(String.format("%n\t\t%s", StringUtils.truncate(FormatUtils.format(votersList, IUser::getName, ", "), 30)));
			}
			count++;
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
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