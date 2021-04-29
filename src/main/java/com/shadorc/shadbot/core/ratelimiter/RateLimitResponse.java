package com.shadorc.shadbot.core.ratelimiter;

public record RateLimitResponse(boolean isLimited,
                                boolean shouldBeWarned) {
}
