package me.shadorc.discordbot.events;

import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

@SuppressWarnings("ucd")
public class ShardListener {

	@EventSubscriber
	public void onDisconnectedEvent(DisconnectedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " disconnected (Reason: " + event.getReason().toString() + ").");
	}

	@EventSubscriber
	public void onShardReadyEvent(ShardReadyEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " ready.");
	}

	@EventSubscriber
	public void onReconnectSuccessEvent(ReconnectSuccessEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " reconnected.");
	}

	@EventSubscriber
	public void onResumedEvent(ResumedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " resumed.");
		Scheduler.sendMsgWaitingForShard();
	}
}