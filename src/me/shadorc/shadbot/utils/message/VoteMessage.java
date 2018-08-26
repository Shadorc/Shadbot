package me.shadorc.shadbot.utils.message;

import java.time.Duration;
import java.util.List;
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

public class VoteMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final int seconds;
	private final List<ReactionEmoji> reactions;

	public VoteMessage(DiscordClient client, Snowflake channelId, int seconds, List<ReactionEmoji> reactions) {
		this.client = client;
		this.channelId = channelId;
		this.seconds = seconds;
		this.reactions = reactions;
	}

	/**
	 * @param embed - the embed to send
	 * @return A {@link Mono} containing a {@link Set} of the message's {@link Reaction}. If an error is received, it is emitted through the {@code Mono}.
	 *         For example, if the message is deleted during the delay, a {@code 404 Forbidden} will be thrown.
	 */
	public Mono<Set<Reaction>> sendMessage(EmbedCreateSpec embed) {
		return BotUtils.sendMessage(embed, client.getMessageChannelById(channelId))
				.flatMap(message -> Flux.fromIterable(reactions)
						.flatMap(message::addReaction)
						.then(Mono.delay(Duration.ofSeconds(seconds)))
						.then(client.getMessageById(channelId, message.getId()))
						.map(Message::getReactions));
	}

}
