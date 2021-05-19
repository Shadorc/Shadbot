package com.locibot.locibot.core.ratelimiter;

import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final Map<Snowflake, LimitedGuild> guildsLimitedMap;
    private final Bandwidth bandwidth;

    /**
     * Specifies simple limitation {@code capacity} tokens per {@code period} time window.
     *
     * @param capacity Maximum amount of tokens.
     * @param period   The period within tokens will be fully regenerated.
     */
    public RateLimiter(int capacity, Duration period) {
        this(Bandwidth.classic(capacity, Refill.intervally(capacity, period)));
    }

    /**
     * Specifies limitation with the provided bandwidth.
     *
     * @param bandwidth The bandwidth.
     */
    public RateLimiter(Bandwidth bandwidth) {
        this.guildsLimitedMap = new ConcurrentHashMap<>();
        this.bandwidth = bandwidth;
    }

    public RateLimitResponse isLimited(Snowflake guildId, Snowflake userId) {
        final LimitedUser limitedUser = this.guildsLimitedMap
                .computeIfAbsent(guildId, __ -> new LimitedGuild(this.bandwidth))
                .getUser(userId);

        // The token could not been consumed, the user is limited
        if (!limitedUser.getBucket().tryConsume(1)) {
            // The user has not yet been warned
            if (!limitedUser.isWarned()) {
                limitedUser.warned(true);
                return new RateLimitResponse(true, true);
            }
            return new RateLimitResponse(true, false);
        }

        limitedUser.warned(false);
        return new RateLimitResponse(false, false);
    }

    public String formatRateLimitMessage(Locale locale) {
        final String message = I18nManager.getRandomSpam(locale);
        final String duration = FormatUtil.formatDurationWords(locale,
                Duration.ofNanos(this.bandwidth.getRefillPeriodNanos()));
        return I18nManager.localize(locale, "ratelimit.message")
                .formatted(message, this.bandwidth.getCapacity(), duration);
    }

}
