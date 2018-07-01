package me.shadorc.shadbot.core.ratelimiter;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.bucket4j.Bucket;

public class LimitedUser {

	private final Bucket bucket;
	private AtomicBoolean isWarned;

	public LimitedUser(Bucket bucket) {
		this.bucket = bucket;
		this.isWarned = new AtomicBoolean(false);
	}

	public Bucket getBucket() {
		return bucket;
	}

	public boolean isWarned() {
		return isWarned.get();
	}

	public void warn() {
		isWarned.set(true);
	}

	public void unwarn() {
		isWarned.set(false);
	}

}
