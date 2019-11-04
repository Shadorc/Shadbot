package com.shadorc.shadbot.db.premium.entity;

import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.utils.TimeUtils;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Relic {

    private final String id;
    private final String type;
    private final long duration;
    @Nullable
    private final Long activation;

    public Relic(RelicBean bean) {
        this.id = bean.getId();
        this.type = bean.getType();
        this.duration = bean.getDuration();
        this.activation = bean.getActivation();
    }

    public Relic(String id, String type, long duration) {
        this.id = id;
        this.type = type;
        this.duration = duration;
        this.activation = null;
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public Duration getDuration() {
        return Duration.ofMillis(this.duration);
    }

    public Optional<Instant> getActivation() {
        return Optional.ofNullable(this.activation)
                .map(Instant::ofEpochMilli);
    }

    public boolean isExpired() {
        return this.getActivation()
                .map(TimeUtils::getMillisUntil)
                .map(elapsedMillis -> elapsedMillis >= this.getDuration().toMillis())
                .orElse(false);
    }

}
