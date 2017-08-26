package me.shadorc.discordbot.events;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent.Reason;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.impl.events.shard.ResumedEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;

@SuppressWarnings("ucd")
public class ShardListener {

	private static final List<IShard> CONNECTED_SHARD = new ArrayList<>();

	@EventSubscriber
	public void onDisconnectedEvent(DisconnectedEvent event) {
		if(event.getReason().equals(Reason.LOGGED_OUT)) {
			LogUtils.info("------------------- Shadbot logged out [Version:" + Config.VERSION.toString() + "] -------------------");
		}
		CONNECTED_SHARD.remove(event.getShard());
	}

	@EventSubscriber
	public void onShardReadyEvent(ShardReadyEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	@EventSubscriber
	public void onReconnectSuccessEvent(ReconnectSuccessEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
	}

	@EventSubscriber
	public void onResumedEvent(ResumedEvent event) {
		if(!CONNECTED_SHARD.contains(event.getShard())) {
			CONNECTED_SHARD.add(event.getShard());
		}
		BotUtils.sendQueues();
	}

	public static boolean isShardConnected(IShard shard) {
		return CONNECTED_SHARD.contains(shard);
	}
}