package com.locibot.locibot.core.ratelimiter;

public record RateLimitResponse(boolean isLimited,
                                boolean shouldBeWarned) {
}
