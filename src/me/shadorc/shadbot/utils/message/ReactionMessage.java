package me.shadorc.shadbot.utils.message;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactionMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final Duration delay;
	private final Collection<ReactionEmoji> reactions;

	public ReactionMessage(DiscordClient client, Snowflake channelId, Collection<ReactionEmoji> collection) {
		this.client = client;
		this.channelId = channelId;
		this.delay = Duration.ZERO;
		this.reactions = collection;
	}

	public ReactionMessage(DiscordClient client, Snowflake channelId, int seconds, Collection<ReactionEmoji> collection) {
		this.client = client;
		this.channelId = channelId;
		this.delay = Duration.ofSeconds(seconds);
		this.reactions = collection;
	}

	/**
	 * @param embed - the embed to send
	 * @return A {@link Mono} containing a {@link Set} of the message's {@link Reaction}. If an error is received, it is emitted through the {@code Mono}.
	 *         For example, if the message is deleted during the delay, a {@code 404 Forbidden} will be thrown.
	 */
	public Mono<Set<Reaction>> sendMessage(EmbedCreateSpec embed) {
		return BotUtils.sendMessage(embed, this.client.getMessageChannelById(this.channelId))
				// Add the reactions to the message then wait
				.flatMap(message -> Flux.fromIterable(this.reactions)
						.flatMap(message::addReaction)
						.then(Mono.delay(delay))
						.thenReturn(message.getId()))
				// Request to retrieve the message then return its reactions
				.flatMap(id -> this.client.getMessageById(this.channelId, id)
						.map(Message::getReactions));
	}

}
