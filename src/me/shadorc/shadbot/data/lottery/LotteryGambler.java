package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

public class LotteryGambler {

	@JsonProperty("guild_id")
	private final Long guildId;
	@JsonProperty("user_id")
	private final Long userId;
	@JsonProperty("number")
	private final int number;

	public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId.asLong();
		this.userId = userId.asLong();
		this.number = number;
	}

	public LotteryGambler() {
		this(Snowflake.of(0L), Snowflake.of(0L), 0);
	}

	@JsonIgnore
	public Snowflake getGuildId() {
		return Snowflake.of(this.guildId);
	}

	@JsonIgnore
	public Snowflake getUserId() {
		return Snowflake.of(this.userId);
	}

	@JsonIgnore
	public int getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		return String.format("LotteryGambler [guildId=%s, userId=%s, number=%s]", guildId, userId, number);
	}

}
