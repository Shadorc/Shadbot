package me.shadorc.shadbot.utils.executor;

import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.internal.DiscordUtils;

public class ShadbotCachedExecutor extends ThreadPoolExecutor {

	public ShadbotCachedExecutor(String threadName) {
		super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), DiscordUtils.createDaemonThreadFactory(threadName));
	}

	@Override
	public void execute(Runnable command) {
		super.execute(this.wrapRunnable(command));
	}

	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(this.wrapRunnable(task));
	}

	private Runnable wrapRunnable(Runnable command) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					command.run();
				} catch (Exception err) {
					LogUtils.error(err, String.format("{%s} An unknown exception occurred while running a task.",
							ShadbotCachedExecutor.class.getSimpleName()));
					throw new RuntimeException(err);
				}
			}
		};
	}

}