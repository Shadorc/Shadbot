package me.shadorc.discordbot.utils.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.game.LottoCmd;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.schedule.ScheduledMessage.Reason;
import sx.blah.discord.handle.obj.IChannel;

public class Scheduler {

	private static final List<ScheduledMessage> MESSAGE_QUEUE = Collections.synchronizedList(new ArrayList<>());
	private static final ScheduledExecutorService SCHEDULED_EXECUTOR =
			Executors.newScheduledThreadPool(2, Utils.getThreadFactoryNamed("Shadbot-Scheduler-%d"));

	public static void start() {
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> DatabaseManager.save(), 1, 1, TimeUnit.MINUTES);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> StatsManager.save(), 5, 5, TimeUnit.MINUTES);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> LottoDataManager.save(), 5, 5, TimeUnit.MINUTES);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help | "
				+ TextUtils.getTip()), 0, 30, TimeUnit.MINUTES);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> NetUtils.postStats(), 2, 2, TimeUnit.HOURS);
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> LottoCmd.lotteryDraw(), LottoCmd.getDelayBeforeNextDraw(),
				TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);
	}

	public static void scheduleMessage(Object message, IChannel channel, Reason reason) {
		ScheduledMessage scheduledMsg = new ScheduledMessage(message, channel, reason);

		if(MESSAGE_QUEUE.contains(scheduledMsg)) {
			return;
		}

		MESSAGE_QUEUE.add(scheduledMsg);

		// Wait for shard to be ready to send them
		if(reason.equals(Reason.SHARD_NOT_READY)) {
			return;
		}

		Shadbot.getDefaultThreadPool().submit(() -> {
			Scheduler.waitAndSend(scheduledMsg);
			MESSAGE_QUEUE.remove(scheduledMsg);
		});
	}

	public static void sendMsgWaitingForShard() {
		Iterator<ScheduledMessage> msgItr = MESSAGE_QUEUE.iterator();
		while(msgItr.hasNext()) {
			ScheduledMessage message = msgItr.next();
			if(message.getReason().equals(Reason.SHARD_NOT_READY)) {
				Shadbot.getDefaultThreadPool().submit(() -> {
					Scheduler.waitAndSend(message);
					msgItr.remove();
				});
			}
		}
	}

	private static void waitAndSend(ScheduledMessage message) {
		int count = 0;
		boolean success = false;
		while(!success && count < Config.MESSAGE_RETRY_COUNT) {
			Utils.sleep(TimeUnit.SECONDS.toMillis(Config.MESSAGE_RETRY_INTERVAL));
			if(Shadbot.getClient().getGuildByID(message.getGuildID()) == null) {
				LogUtils.info("{Guild ID: " + message.getGuildID() + "} Shadbot is no longer in this guild, abort attempt to send message.");
				return;
			}
			LogUtils.info("{Guild ID: " + message.getGuildID() + "} Sending pending message...");
			success = message.send() != null;
			count++;
		}

		if(success) {
			LogUtils.info("{Guild ID: " + message.getGuildID() + "} Pending message sent.");
		} else {
			LogUtils.info("{Guild ID: " + message.getGuildID() + "} Too many try, abort attempt to send message.");
		}
	}

	public static void stop() {
		if(SCHEDULED_EXECUTOR.isShutdown()) {
			return;
		}

		try {
			SCHEDULED_EXECUTOR.submit(() -> {
				DatabaseManager.save();
				StatsManager.save();
				LottoDataManager.save();
			}).get();
			SCHEDULED_EXECUTOR.shutdownNow();
		} catch (InterruptedException | ExecutionException err) {
			LogUtils.error("An error occurred while stopping scheduler.", err);
		}
	}
}
