package me.shadorc.shadbot.command.utils.poll;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.message.UpdateableMessage;
import reactor.core.publisher.Flux;

class PollManager extends AbstractGameManager {

	private final PollCreateSpec spec;
	private final List<Voter> voters;
	private final UpdateableMessage message;

	private long startTime;

	public PollManager(Context context, PollCreateSpec spec) {
		super(context);
		this.spec = spec;
		this.voters = new ArrayList<>();
		this.message = new UpdateableMessage(context.getClient(), context.getChannelId());
	}

	@Override
	public void start() {
		this.schedule(this::stop, spec.getDuration(), TimeUnit.SECONDS);
		startTime = System.currentTimeMillis();
		this.show();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		PollCmd.MANAGER.remove(this.getContext().getChannelId());
		voters.clear();
		this.show();
	}

	protected synchronized void vote(Snowflake userId, int num) {
		voters.stream()
				.filter(voter -> voter.getId().equals(userId))
				.findFirst()
				.ifPresent(voter -> voter.setChoice(spec.getChoices().get(num - 1)));
		this.show();
	}

	protected void show() {
		// FIXME: Is the author always the creator ?
		this.getContext().getAuthor().subscribe(author -> {
			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
					.setAuthor(String.format("Poll (Created by: %s)", author.getUsername()),
							null,
							DiscordUtils.getAvatarUrl(author))
					.setThumbnail(author.getAvatarHash().get())
					.setDescription(String.format("Vote using: `%s%s <choice>`%n%n__**%s**__%s",
							this.getContext().getPrefix(), this.getContext().getCommandName(),
							spec.getQuestion(), this.getRepresentation()))
					.setFooter(null, "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png");

			if(this.isTaskDone()) {
				embed.setFooter("Finished", null);
			} else {
				final long elapsedTime = TimeUnit.SECONDS.toMillis(spec.getDuration()) - TimeUtils.getMillisUntil(startTime);
				embed.setFooter(String.format("Time left: %s", FormatUtils.formatShortDuration(elapsedTime)), null);
			}

			message.send(embed);
		});
	}

	private String getRepresentation() {
		StringBuilder representation = new StringBuilder();

		int count = 0;
		for(String choice : spec.getChoices()) {
			count++;
			representation.append(String.format("%n\t**%d.** %s", count, choice));

			Flux.fromIterable(this.voters)
					.filter(voter -> voter.getChoice().equals(choice))
					.flatMap(voter -> this.getContext().getClient().getUserById(voter.getId()))
					.buffer()
					.subscribe(users -> {
						// FIXME: Do this inside a subscribe method is probably a very bad idea
						representation.append(String.format(" *(Vote: %d)*", users.size()));
						representation.append(String.format("%n\t\t%s", StringUtils.truncate(FormatUtils.format(users, User::getUsername, ", "), 30)));
					});
		}

		return representation.toString();
	}

	public int getChoicesCount() {
		return spec.getChoices().size();
	}
}