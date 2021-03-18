package com.shadorc.shadbot.core.ratelimiter;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    public Mono<Boolean> isLimitedAndWarn(Context context) {
        final LimitedUser limitedUser = this.guildsLimitedMap
                .computeIfAbsent(context.getGuildId(), __ -> new LimitedGuild(this.bandwidth))
                .getUser(context.getAuthorId());

        // The token could not been consumed, the user is limited
        if (!limitedUser.getBucket().tryConsume(1)) {
            // The user has not yet been warned
            if (!limitedUser.isWarned()) {
                limitedUser.warned(true);
                return this.sendWarningMessage(context)
                        .thenReturn(true);
            }
            return Mono.just(true);
        }

        limitedUser.warned(false);
        return Mono.just(false);
    }

    private Mono<Snowflake> sendWarningMessage(Context context) {
        final String message = ShadbotUtil.SPAMS.getRandomLine();
        final String duration = FormatUtil.formatDurationWords(
                Duration.ofNanos(this.bandwidth.getRefillPeriodNanos()));
        return context.reply(Emoji.STOPWATCH, context.localize("ratelimit.message")
                .formatted(message, this.bandwidth.getCapacity(), duration));
    }

}
