package me.shadorc.shadbot.command.utils.poll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.game.AbstractGameManager;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.UpdateableMessage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class PollManager extends AbstractGameManager {

	private final Map<String, List<IUser>> choicesMap;

	private final int duration;
	private final String question;
	private final UpdateableMessage message;

	private long startTime;

	protected PollManager(AbstractCommand cmd, IChannel channel, IUser author, int duration, String question, List<String> choicesList) {
		super(cmd, channel, author);
		this.duration = duration;
		this.question = question;
		this.message = new UpdateableMessage(channel);

		this.choicesMap = new LinkedHashMap<String, List<IUser>>();
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

			StringBuilder votersDesc = new StringBuilder(StringUtils.truncate(FormatUtils.format(votersList, IUser::getName, ", "), 30));
			if(votersDesc.length() > 0) {
				votersDesc.insert(0, String.format(" *(Vote: %d)*%n\t\t", votersList.size()));
			}

			choicesStr.append(String.format("%n\t**%d.** %s%s", count, choice, votersDesc.toString()));
			count++;
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName(String.format("Poll (Created by: %s)", this.getAuthor().getName()))
				.withThumbnail(this.getAuthor().getAvatarURL())
				.appendDescription("Vote using: `" + Database.getDBGuild(this.getGuild()).getPrefix() + "poll <choice>`"
						+ "\n\n__**" + question + "**__"
						+ choicesStr.toString())
				.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png");

		if(this.isTaskDone()) {
			embed.withFooterText("Finished");
		} else {
			embed.withFooterText(String.format("Time left: %s",
					FormatUtils.formatShortDuration(TimeUnit.SECONDS.toMillis(duration) - DateUtils.getMillisUntil(startTime))));
		}

		message.send(embed.build()).get();
	}

	public int getChoicesCount() {
		return choicesMap.size();
	}
}