package me.shadorc.shadbot.utils.message;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

	private Snowflake messageId;

	public VoteMessage(DiscordClient client, Snowflake channelId, int seconds) {
		this.client = client;
		this.channelId = channelId;
		this.seconds = seconds;
	}

	public Mono<Map<Reaction, Integer>> sendMessage(EmbedCreateSpec embed) {
		return BotUtils.sendMessage(embed, client.getMessageChannelById(channelId))
				.flatMap(message -> message.addReaction(ReactionEmoji.unicode("✅"))
						.then(message.addReaction(ReactionEmoji.unicode("❌")))
						.thenReturn(message.getId()))
				.map(messageId -> this.messageId = messageId)
				.then(Mono.delay(Duration.ofSeconds(seconds)))
				.then(client.getMessageById(channelId, messageId))
				.then(this.count());
	}

	private Mono<Map<Reaction, Integer>> count() {
		return client.getMessageById(channelId, messageId)
				.map(Message::getReactions)
				.flatMapMany(Flux::fromIterable)
				.collectList()
				.map(reactions -> {
					final Map<Reaction, Integer> reactionsMap = new HashMap<>();
					for(Reaction reaction : reactions) {
						reactionsMap.put(reaction, reactionsMap.getOrDefault(reactionsMap, 0) + 1);
					}
					return reactionsMap;
				});
	}

}
