package me.shadorc.shadbot.listener;

import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import me.shadorc.shadbot.utils.TimeUtils;

public class MessageUpdateListener {

	public static void onMessageUpdateEvent(MessageUpdateEvent event) {
		if(!event.isContentChanged() || !event.getOld().isPresent() || !event.getGuildId().isPresent()) {
			return;
		}

		final Message oldMessage = event.getOld().get();

		// If the message has been sent more than 30 seconds ago, ignore it
		if(TimeUtils.getMillisUntil(oldMessage.getTimestamp()) > TimeUnit.SECONDS.toMillis(30)) {
			return;
		}

		event.getMessage()
				.zipWith(event.getMessage().flatMap(Message::getAuthorAsMember))
				.subscribe(messageAndMember -> {
					final Message message = messageAndMember.getT1();
					final Member member = messageAndMember.getT2();
					final Long guildId = event.getGuildId().get().asLong();
					MessageCreateListener.onMessageCreate(new MessageCreateEvent(event.getClient(), message, guildId, member));
				});
	}

}
