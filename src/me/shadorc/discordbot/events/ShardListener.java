package me.shadorc.discordbot.events;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

@SuppressWarnings("ucd")
public class ShardListener {

	private static final List<IShard> CONNECTED_SHARD = new ArrayList<>();

	@EventSubscriber
	public void onDisconnectedEvent(DisconnectedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " disconnected (Reason: " + event.getReason().toString() + ").");
		CONNECTED_SHARD.remove(event.getShard());
	}

	@EventSubscriber
	public void onShardReadyEvent(ShardReadyEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " ready.");
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	@EventSubscriber
	public void onReconnectSuccessEvent(ReconnectSuccessEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " reconnected.");
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	@EventSubscriber
	public void onResumedEvent(ResumedEvent event) {
		LogUtils.info("Shard " + event.getShard().getInfo()[0] + " resumed.");
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
		Scheduler.sendMsgWaitingForShard();
	}

	public static boolean isShardConnected(IShard shard) {
		return CONNECTED_SHARD.contains(shard);
	}
}