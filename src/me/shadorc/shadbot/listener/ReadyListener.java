package me.shadorc.shadbot.listener;

import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.StatusType;

public class ReadyListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Shadbot.VERSION);

		ShardManager.start();

		Shadbot.getScheduler().scheduleAtFixedRate(() -> LottoCmd.draw(), LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);
		Shadbot.getScheduler().scheduleAtFixedRate(() -> BotUtils.updatePresence(), 0, 30, TimeUnit.MINUTES);
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
