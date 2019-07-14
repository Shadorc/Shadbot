package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import com.shadorc.shadbot.data.stats.StatsManager;
import com.shadorc.shadbot.data.stats.enums.VariousEnum;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class MessageCreateListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return Mono.fromRunnable(() -> StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_RECEIVED))
                .and(CommandProcessor.getInstance().processMessageEvent(event));
    }

}