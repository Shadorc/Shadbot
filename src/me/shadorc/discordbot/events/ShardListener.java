package me.shadorc.discordbot.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent.Reason;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

@SuppressWarnings("ucd")
public class ShardListener {

	private static final List<IShard> CONNECTED_SHARD = new ArrayList<>();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	@EventSubscriber
	public void onShardEvent(ShardEvent event) {
		if(event instanceof DisconnectedEvent) {
			executor.execute(() -> this.onDisconnected((DisconnectedEvent) event));
		}
		if(event instanceof ShardReadyEvent) {
			executor.execute(() -> this.onShardReady((ShardReadyEvent) event));
		}
		if(event instanceof ReconnectSuccessEvent) {
			executor.execute(() -> this.onReconnectSuccess((ReconnectSuccessEvent) event));
		}
		if(event instanceof ResumedEvent) {
			executor.execute(() -> this.onResumed((ResumedEvent) event));
		}
	}

	private void onDisconnected(DisconnectedEvent event) {
		if(event.getReason().equals(Reason.LOGGED_OUT)) {
			LogUtils.info("------------------- Shadbot logged out [Version:" + Config.VERSION.toString() + "] -------------------");
		}
		CONNECTED_SHARD.remove(event.getShard());
	}

	private void onShardReady(ShardReadyEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	private void onReconnectSuccess(ReconnectSuccessEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	private void onResumed(ResumedEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
		Scheduler.sendMsgWaitingForShard();
	}

	public static boolean isShardConnected(IShard shard) {
		return CONNECTED_SHARD.contains(shard);
	}
}