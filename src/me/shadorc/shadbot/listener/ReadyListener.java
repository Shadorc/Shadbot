package me.shadorc.shadbot.listener;

import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;

public class ReadyListener implements Consumer<ReadyEvent> {

	@Override
	public void accept(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Shadbot.VERSION);

		ShardManager.start();

		Shadbot.getScheduler().scheduleAtFixedRate(() -> LottoCmd.draw(), LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);
		Shadbot.getScheduler().scheduleAtFixedRate(() -> BotUtils.updatePresence(), 1, 30, TimeUnit.MINUTES);
		Shadbot.getScheduler().scheduleAtFixedRate(() -> NetUtils.postStats(), 2, 2, TimeUnit.HOURS);

		event.getClient().getDispatcher().registerListeners(Shadbot.getEventThreadPool(),
				new ChannelListener(),
				new GuildListener(),
				new GuildMemberListener(),
				new MessageListener(),
				new UserVoiceChannelListener(),
				new VoiceChannelListener());

		event.getClient().changePresence(StatusType.ONLINE);		
	}

}
