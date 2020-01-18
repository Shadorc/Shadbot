package com.shadorc.shadbot.core.ratelimiter;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.TemporaryMessage;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final Map<Snowflake, LimitedGuild> guildsLimitedMap;
    private final Bandwidth bandwidth;

    /**
     * Specifies simple limitation {@code capacity} tokens per {@code period} time window.
     *
     * @param capacity - maximum amount of tokens
     * @param period   - the period within tokens will be fully regenerated
     */
    public RateLimiter(int capacity, Duration period) {
        this(Bandwidth.classic(capacity, Refill.intervally(capacity, period)));
    }

    /**
     * Specifies limitation with the provided bandwidth.
     *
     * @param bandwidth - the bandwidth
     */
    public RateLimiter(Bandwidth bandwidth) {
        this.guildsLimitedMap = new ConcurrentHashMap<>();
        this.bandwidth = bandwidth;
    }

    public boolean isLimitedAndWarn(Snowflake channelId, Member member) {
        final LimitedUser limitedUser = this.guildsLimitedMap.computeIfAbsent(member.getGuildId(),
                ignored -> new LimitedGuild(this.bandwidth))
                .getUser(member.getId());

        // The token could not been consumed, the user is limited
        if (!limitedUser.getBucket().tryConsume(1)) {
            // The user has not yet been warned
            if (!limitedUser.isWarned()) {
                this.sendWarningMessage(member.getClient(), channelId, member.getId());
                limitedUser.warn();
            }
            return true;
        }

        limitedUser.unwarn();
        return false;
    }

    private void sendWarningMessage(GatewayDiscordClient client, Snowflake channelId, Snowflake userId) {
        client.getUserById(userId)
                .map(author -> {
                    final String username = author.getUsername();
                    final String message = TextUtils.SPAMS.getRandomText();
                    final String maxNum = StringUtils.pluralOf(this.bandwidth.getCapacity(), "time");
                    final String durationStr = DurationFormatUtils.formatDurationWords(
                            this.bandwidth.getRefillPeriodNanos() / 1_000_000, true, true);
                    return String.format(Emoji.STOPWATCH + " (**%s**) %s You can use this command %s every *%s*.",
                            username, message, maxNum, durationStr);
                })
                .flatMap(new TemporaryMessage(client, channelId, Duration.ofSeconds(10))::send)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

}
