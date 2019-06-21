package me.shadorc.shadbot.data.premium;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.TimeUtils;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Relic {

    public enum RelicType {
        USER, GUILD;
    }

    @JsonProperty("id")
    private final String id;
    @Nullable
    @JsonProperty("guildId")
    private long guildId;
    @Nullable
    @JsonProperty("userId")
    private long userId;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("duration")
    private final long duration;
    @Nullable
    @JsonProperty("activationTime")
    private long activationTime;

    public Relic(String id, long duration, RelicType type) {
        this.id = id;
        this.duration = duration;
        this.type = type.toString();
    }

    public Relic() {
        this.id = null;
        this.duration = 0;
        this.type = null;
    }

    public void activate(Snowflake userId) {
        this.userId = userId.asLong();
        this.activationTime = System.currentTimeMillis();
    }

    public String getId() {
        return this.id;
    }

    /**
     * @return The guild ID of this relic, if present
     */
    public Optional<Snowflake> getGuildId() {
        return Optional.ofNullable(this.guildId).map(Snowflake::of);
    }

    /**
     * @return The user ID of this relic, if present
     */
    public Optional<Snowflake> getUserId() {
        return Optional.ofNullable(this.userId).map(Snowflake::of);
    }

    public String getType() {
        return this.type;
    }

    /**
     * @return The duration of this relic
     */
    public Duration getDuration() {
        return Duration.ofMillis(this.duration);
    }

    /**
     * @return The activation time of this relic, if activated
     */
    public Optional<Instant> getActivationInstant() {
        return Optional.ofNullable(this.activationTime).map(Instant::ofEpochMilli);
    }

    public boolean isExpired() {
        return this.getActivationInstant()
                .map(instant -> TimeUtils.getMillisUntil(instant) >= this.getDuration().toMillis())
                .orElse(false);
    }

    public void setGuildId(Snowflake guildId) {
        this.guildId = guildId.asLong();
    }

    public void setUserId(Snowflake userId) {
        this.userId = userId.asLong();
    }

}
