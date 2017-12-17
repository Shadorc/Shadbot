package me.shadorc.discordbot.events;

import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

@SuppressWarnings("ucd")
public class ShardListener {

	@EventSubscriber
	public void onShardEvent(ShardEvent event) {
		if(event instanceof DisconnectedEvent) {
			this.onDisconnectedEvent((DisconnectedEvent) event);
		} else if(event instanceof ShardReadyEvent) {
			this.onShardReadyEvent((ShardReadyEvent) event);
		} else if(event instanceof ReconnectSuccessEvent) {
			this.onReconnectSuccessEvent((ReconnectSuccessEvent) event);
		} else if(event instanceof ResumedEvent) {
			this.onResumedEvent((ResumedEvent) event);
		}
	}

	private void onDisconnectedEvent(DisconnectedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " disconnected (Reason: " + event.getReason().toString() + ").");
	}

	private void onShardReadyEvent(ShardReadyEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " ready.");
	}

	private void onReconnectSuccessEvent(ReconnectSuccessEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " reconnected.");
	}

	private void onResumedEvent(ResumedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " resumed.");
		Scheduler.sendMsgWaitingForShard();
	}
}