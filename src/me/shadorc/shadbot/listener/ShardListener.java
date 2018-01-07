package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

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
		LogUtils.infof("Shard %d disconnected (Reason: %s).", event.getShard().getInfo()[0], event.getReason().toString());
	}

	private void onShardReadyEvent(ShardReadyEvent event) {
		LogUtils.infof("Shard %d ready.", event.getShard().getInfo()[0]);
		ShardManager.addShardIfAbsent(event.getShard());
	}

	private void onReconnectSuccessEvent(ReconnectSuccessEvent event) {
		LogUtils.infof("Shard %d reconnected.", event.getShard().getInfo()[0]);
	}

	private void onResumedEvent(ResumedEvent event) {
		LogUtils.infof("Shard %d resumed.", event.getShard().getInfo()[0]);
		ShardManager.getShadbotShard(event.getShard()).sendQueue();
	}
}