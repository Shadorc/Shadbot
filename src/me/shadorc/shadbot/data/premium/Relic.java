package me.shadorc.shadbot.data.premium;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;

public class Relic {

	private static final String RELIC_ID = "relicID";
	private static final String ACTIVATION = "activationMillis";
	private static final String DURATION = "duration";
	private static final String TYPE = "type";
	private static final String GUILD_ID = "guildID";

	private final Snowflake id;
	private final int duration;
	private final RelicType type;

	private long activationTime;
	private Snowflake guildId;

	public Relic(Snowflake relicId, int duration, RelicType type) {
		this.id = relicId;
		this.duration = duration;
		this.type = type;
	}

	public Relic(JSONObject relicObj) {
		this.id = Snowflake.of(relicObj.getString(RELIC_ID));
		this.duration = relicObj.getInt(DURATION);
		this.type = Utils.getValueOrNull(RelicType.class, relicObj.get(TYPE).toString());
		this.activationTime = relicObj.optLong(ACTIVATION);
	}

	public void activate() {
		this.activationTime = System.currentTimeMillis();
	}

	public Snowflake getId() {
		return id;
	}

	public int getDuration() {
		return duration;
	}

	public RelicType getType() {
		return type;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public long getActivationTime() {
		return activationTime;
	}

	public void setGuildId(Snowflake guildId) {
		this.guildId = guildId;
	}

	public boolean isExpired() {
		return TimeUnit.MILLISECONDS.toDays(TimeUtils.getMillisUntil(this.getActivationTime())) >= this.getDuration();
	}

	public JSONObject toJSON() {
		return new JSONObject()
				.put(RELIC_ID, this.getId().asLong())
				.put(DURATION, this.getDuration())
				.put(TYPE, this.getType())
				.putOpt(ACTIVATION, this.getActivationTime())
				.putOpt(GUILD_ID, this.getGuildId().asLong());
	}

}
