package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import me.shadorc.shadbot.core.command.CommandProcessor;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;

public class MessageCreateListener {

	public static void onMessageCreate(MessageCreateEvent event) {
		StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_RECEIVED);
		CommandProcessor.processMessageEvent(event);
	}

}