package me.shadorc.shadbot.data.premium;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class Relic {

	private final static String RELIC_ID = "relicID";
	private final static String ACTIVATION = "activationMillis";
	private final static String DURATION = "duration";
	private final static String TYPE = "type";
	private static final String GUILD_ID = "guildID";

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
		this.duration = relicObj.getInt(DURATION);
		this.type = RelicType.valueOf(relicObj.getString(TYPE));
	}

	public void activate() {
		this.activationTime = System.currentTimeMillis();
		PremiumManager.save(this);
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

	public void setGuildID(long guildID) {
		this.guildID = guildID;
		PremiumManager.save(this);
	}

	public JSONObject toJSON() {
		return new JSONObject()
				.put(RELIC_ID, this.getID())
				.put(DURATION, this.getDuration())
				.put(TYPE, this.getType())
				.putOpt(ACTIVATION, this.getActivationTime())
				.putOpt(GUILD_ID, this.getGuildID());
	}

}
