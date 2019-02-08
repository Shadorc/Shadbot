package me.shadorc.shadbot.command.utils.poll;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.message.ReactionMessage;
import reactor.core.publisher.Mono;

public class PollManager extends AbstractGameManager {

	private final PollCreateSpec spec;
	private final ReactionMessage voteMessage;

	public PollManager(Context context, PollCreateSpec spec) {
		super(context);
		this.spec = spec;
		this.voteMessage = new ReactionMessage(context.getClient(), context.getChannelId(), spec.getChoices().values());
	}

	@Override
	public void start() {
		this.schedule(Mono.fromRunnable(this::stop), this.spec.getDuration().toMillis(), ChronoUnit.MILLIS);
		this.show()
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err));
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		PollCmd.MANAGER.remove(this.getContext().getChannelId());
	}

	@Override
	public Mono<Void> show() {
		final StringBuilder representation = new StringBuilder();
		for(int i = 0; i < this.spec.getChoices().size(); i++) {
			representation.append(String.format("%n\t**%d.** %s", i + 1, this.spec.getChoices().keySet().toArray()[i]));
		}

		final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.setAuthor(String.format("Poll by %s)", this.getContext().getUsername()),
						null, this.getContext().getAvatarUrl())
						.setDescription(String.format("Vote using: `%s%s <choice>`%n%n__**%s**__%s",
								this.getContext().getPrefix(), this.getContext().getCommandName(),
								this.spec.getQuestion(), representation.toString()))
						.setFooter(String.format("You have %s to vote.",
								FormatUtils.shortDuration(this.spec.getDuration().toMillis())),
								"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png"));

		return this.voteMessage.send(embedConsumer)
				.flatMap(message -> Mono.delay(this.spec.getDuration())
						.thenReturn(message.getId()))
				.flatMap(messageId -> this.getContext().getClient().getMessageById(this.getContext().getChannelId(), messageId))
				.map(Message::getReactions)
				.flatMap(this::sendResults)
				.then();
	}

	private Mono<Message> sendResults(Set<Reaction> reactions) {
		// Reactions are not in the same order as they were when added to the message, they need to be ordered
		final BiMap<ReactionEmoji, String> reactionsChoices = HashBiMap.create(this.spec.getChoices()).inverse();
		final Map<String, Integer> choicesVotes = new HashMap<>();
		for(final Reaction reaction : reactions) {
			// -1 is here to ignore the reaction of the bot itself
			choicesVotes.put(reactionsChoices.get(reaction.getEmoji()), reaction.getCount() - 1);
		}

		// Sort votes map by value in the ascending order
		final StringBuilder representation = new StringBuilder();
		int count = 1;
		for(final String key : Utils.sortByValue(choicesVotes, Collections.reverseOrder(Entry.comparingByValue())).keySet()) {
			representation.append(String.format("%n\t**%d.** %s (Votes: %d)", count, key, choicesVotes.get(key)));
			count++;
		}

		final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.setAuthor(String.format("Poll results (Author: %s)", this.getContext().getUsername()),
						null, this.getContext().getAvatarUrl())
						.setDescription(String.format("__**%s**__%s", this.spec.getQuestion(), representation.toString())));

		return this.getContext().getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
	}

}