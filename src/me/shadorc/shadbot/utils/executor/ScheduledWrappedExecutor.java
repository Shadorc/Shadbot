package me.shadorc.shadbot.utils.executor;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class ScheduledWrappedExecutor extends ScheduledThreadPoolExecutor {

	/**
	 * A default scheduled thread pool with tasks wrapped to catch {@link Exception} and {@code removeOnCancelPolicy} set to {@code true}
	 * 
	 * @param corePoolSize - the number of threads to keep in the pool, even if they are idle
	 * @param threadName - the thread name
	 */
	public ScheduledWrappedExecutor(int corePoolSize, String threadName) {
		super(corePoolSize, Utils.createDaemonThreadFactory(threadName));
		this.setRemoveOnCancelPolicy(true);
	}

	/**
	 * A default scheduled thread pool with a single core pool size, tasks wrapped to catch {@link Exception} and {@code removeOnCancelPolicy} set to
	 * {@code true}
	 * 
	 * @param threadName - the thread name
	 */
	public ScheduledWrappedExecutor(String name) {
		this(1, name);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return super.scheduleAtFixedRate(this.wrapRunnable(command), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return super.scheduleWithFixedDelay(this.wrapRunnable(command), initialDelay, delay, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return super.schedule(this.wrapRunnable(command), delay, unit);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(this.wrapRunnable(task));
	}

	public <T> Mono<T> scheduleAtFixedRate(Mono<T> mono, long initialDelay, long period, TimeUnit unit) {
		return Mono.fromRunnable(() -> super.scheduleAtFixedRate(this.wrapRunnable(mono::subscribe), initialDelay, period, unit));
	}

	public <T> Mono<T> scheduleWithFixedDelay(Mono<T> mono, long initialDelay, long delay, TimeUnit unit) {
		return Mono.fromRunnable(() -> super.scheduleWithFixedDelay(this.wrapRunnable(mono::subscribe), initialDelay, delay, unit));
	}

	public <T> Mono<T> schedule(Mono<T> mono, long delay, TimeUnit unit) {
		return Mono.fromRunnable(() -> super.schedule(this.wrapRunnable(mono::subscribe), delay, unit));
	}

	public <T> Mono<T> submit(Mono<T> mono) {
		return Mono.fromRunnable(() -> super.submit(this.wrapRunnable(mono::subscribe)));
	}

	private Runnable wrapRunnable(Runnable command) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					command.run();
				} catch (Exception err) {
					LogUtils.error(err, String.format("{%s} An unknown exception occurred while running a scheduled task.",
							ScheduledWrappedExecutor.class.getSimpleName()));
					throw new RuntimeException(err);
				}
			}
		};
	}

}