package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import me.shadorc.shadbot.core.command.CommandProcessor;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import reactor.core.publisher.Mono;

public class MessageCreateListener {

	public static Mono<Void> onMessageCreate(MessageCreateEvent event) {
		return Mono.fromRunnable(() -> StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_RECEIVED))
				.and(CommandProcessor.processMessageEvent(event));
	}

}