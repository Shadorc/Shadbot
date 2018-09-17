package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		LogUtils.infof("{Shard %d} %s",
				event.getClient().getConfig().getShardIndex(),
				event.toString());
	}

}