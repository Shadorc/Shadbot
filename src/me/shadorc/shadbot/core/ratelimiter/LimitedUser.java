package me.shadorc.shadbot.core.ratelimiter;

import io.github.bucket4j.Bucket;

public class LimitedUser {

	private final Bucket bucket;
	private boolean isWarned;

	public LimitedUser(Bucket bucket) {
		this.bucket = bucket;
		this.isWarned = false;
	}

	public Bucket getBucket() {
		return bucket;
	}

	public boolean isWarned() {
		return isWarned;
	}

	public void warn() {
		this.isWarned = true;
	}

	public void unwarn() {
		this.isWarned = false;
	}

}
