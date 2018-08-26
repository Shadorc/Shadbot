package me.shadorc.shadbot.command.utils.poll;

import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.VoteMessage;
import reactor.core.publisher.Mono;

public class PollManager extends AbstractGameManager {

	private final PollCreateSpec spec;
	private final VoteMessage voteMessage;

	private long startTime;

	public PollManager(Context context, PollCreateSpec spec) {
		super(context);
		this.spec = spec;
		this.voteMessage = new VoteMessage(context.getClient(), context.getChannelId(), spec.getDuration(), spec.getReactions());
	}

	@Override
	public void start() {
		this.schedule(Mono.fromRunnable(this::stop), spec.getDuration(), ChronoUnit.SECONDS);
		startTime = System.currentTimeMillis();
		this.show().subscribe();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		PollCmd.MANAGER.remove(this.getContext().getChannelId());
	}

	@Override
	public Mono<Void> show() {
		return this.getContext().getAvatarUrl()
				.map(avatarUrl -> {
					StringBuilder representation = new StringBuilder();
					for(int i = 0; i < spec.getChoices().size(); i++) {
						representation.append(String.format("%n\t**%d.** %s", i + 1, spec.getChoices().get(i)));
					}
					final long elapsedTime = TimeUnit.SECONDS.toMillis(spec.getDuration()) - TimeUtils.getMillisUntil(startTime);

					return EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Poll (Created by: %s)", this.getContext().getUsername()), null, avatarUrl)
							.setDescription(String.format("Vote using: `%s%s <choice>`%n%n__**%s**__%s",
									this.getContext().getPrefix(), this.getContext().getCommandName(),
									spec.getQuestion(), representation.toString()))
							.setFooter(String.format("You have %s to vote.", FormatUtils.formatShortDuration(elapsedTime)),
									"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png");
				})
				.flatMap(voteMessage::sendMessage)
				.flatMap(this::sendResults)
				.then();
	}

	// TODO
	private Mono<Message> sendResults(Set<Reaction> reactions) {
		return Mono.empty();
	}

}