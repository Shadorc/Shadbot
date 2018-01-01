package me.shadorc.shadbot.data.premium;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class Relic {

	private final static String RELIC_ID = "relicID";
	// private final static String RELIC_ACTIVATION_MILLIS = "activationMillis";
	private final static String RELIC_DURATION = "duration";
	private final static String RELIC_TYPE = "type";

	private final String relicID;
	private final int duration;
	private final RelicType type;

	private long activationTime;
	private long guildID;

	public Relic(String relicID, int duration, RelicType type) {
		this.relicID = relicID;
		this.duration = duration;
		this.type = type;
	}

	public Relic(JSONObject relicObj) {
		this.relicID = relicObj.getString(RELIC_ID);
		this.duration = relicObj.getInt(RELIC_DURATION);
		this.type = RelicType.valueOf(relicObj.getString(RELIC_TYPE));
	}

	public String getID() {
		return relicID;
	}

	public int getDuration() {
		return duration;
	}

	public RelicType getType() {
		return type;
	}

	public Long getGuildID() {
		return guildID;
	}

	public long getActivationTime() {
		return activationTime;
	}

	public boolean isExpired() {
		return TimeUnit.MILLISECONDS.toDays(this.getActivationTime() + System.currentTimeMillis()) > this.getDuration();
	}

	public void activate() {
		// TODO: Need to be saved
		this.activationTime = System.currentTimeMillis();
	}

	public void setGuildID(long guildID) {
		this.guildID = guildID;
	}

	public JSONObject toJSON() {
		return new JSONObject()
				.put(RELIC_ID, this.getID())
				.put(RELIC_DURATION, this.getDuration())
				.put(RELIC_TYPE, this.getType());
	}

}
