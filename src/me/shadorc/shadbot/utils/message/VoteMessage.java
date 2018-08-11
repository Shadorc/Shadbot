package me.shadorc.shadbot.utils.message;

import java.time.Duration;

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

	public VoteMessage(DiscordClient client, Snowflake channelId, int seconds) {
		this.client = client;
		this.channelId = channelId;
		this.seconds = seconds;
	}

	/**
	 * @param embed - the embed to send
	 * @return A {@link Flux} that continually emits the message's {@link Reaction}. If an error is received, it is emitted
	 *         through the {@code Flux}. For example, if the message is deleted during the delay, a {@code 404 Forbidden} 
	 *         will be thrown.
	 */
	public Flux<Reaction> sendMessage(EmbedCreateSpec embed) {
		return BotUtils.sendMessage(embed, client.getMessageChannelById(channelId))
				.flatMapMany(message -> message.addReaction(ReactionEmoji.unicode("✅"))
						.then(message.addReaction(ReactionEmoji.unicode("❌")))
						.then(Mono.delay(Duration.ofSeconds(seconds)))
						.then(client.getMessageById(channelId, message.getId()))
						.map(Message::getReactions)
						.flatMapMany(Flux::fromIterable));
	}

}
