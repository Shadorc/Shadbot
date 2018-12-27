package me.shadorc.shadbot.data.premium;

import java.util.Optional;
import java.util.OptionalLong;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.TimeUtils;

public class Relic {

	public enum RelicType {
		USER, GUILD;
	}

	@JsonProperty("id")
	private final String id;
	@Nullable
	@JsonProperty("guildId")
	private Snowflake guildId;
	@Nullable
	@JsonProperty("userId")
	private Snowflake userId;
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
		this.userId = userId;
		this.activationTime = System.currentTimeMillis();
	}

	public String getId() {
		return this.id;
	}

	/**
	 * @return The guild ID of this {@link Relic}, if present
	 */
	public Optional<Snowflake> getGuildId() {
		return Optional.ofNullable(this.guildId);
	}

	/**
	 * @return The user ID of this {@link Relic}, if present
	 */
	public Optional<Snowflake> getUserId() {
		return Optional.ofNullable(this.userId);
	}

	public String getType() {
		return this.type;
	}

	/**
	 * @return The duration of this {@link Relic} in milliseconds
	 */
	public long getDuration() {
		return this.duration;
	}

	/**
	 * @return The activation time of this {@link Relic} in milliseconds if activated
	 */
	public OptionalLong getActivationTime() {
		return OptionalLong.of(this.activationTime);
	}

	public boolean isExpired() {
		if(!this.getActivationTime().isPresent()) {
			return false;
		}
		return TimeUtils.getMillisUntil(this.getActivationTime().getAsLong()) >= this.getDuration();
	}

	public void setGuildId(Snowflake guildId) {
		this.guildId = guildId;
	}

	public void setUserId(Snowflake userId) {
		this.userId = userId;
	}

}
