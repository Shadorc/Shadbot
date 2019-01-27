package me.shadorc.shadbot.utils.object.message;

import java.util.Collection;
import java.util.function.Consumer;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactionMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final Collection<ReactionEmoji> reactions;

	public ReactionMessage(DiscordClient client, Snowflake channelId, Collection<ReactionEmoji> collection) {
		this.client = client;
		this.channelId = channelId;
		this.reactions = collection;
	}

	/**
	 * @param embed - the embed to send
	 * @return A {@link Mono} containing a {@link Message} with {@link Reaction} added. If an error is received, it is emitted through the {@code Mono}.
	 *         For example, if the message is deleted during the delay, a {@code 404 Forbidden} will be thrown.
	 */
	public Mono<Message> sendMessage(Consumer<? super EmbedCreateSpec> embed) {
		return this.client.getChannelById(this.channelId)
				.cast(MessageChannel.class)
				.flatMap(channel -> DiscordUtils.sendMessage(embed, channel))
				// Add the reactions to the message then wait
				.flatMap(message -> Flux.fromIterable(this.reactions)
						.flatMap(message::addReaction)
						.then(Mono.just(message)));
	}

}
