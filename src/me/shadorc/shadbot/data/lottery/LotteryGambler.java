package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

public class LotteryGambler {

	@JsonProperty("guild_id")
	private final Snowflake guildId;
	@JsonProperty("user_id")
	private final Snowflake userId;
	@JsonProperty("number")
	private final int number;

	public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId;
		this.userId = userId;
		this.number = number;
	}

	public LotteryGambler() {
		this(Snowflake.of(0L), Snowflake.of(0L), 0);
	}

	@JsonIgnore
	public Snowflake getGuildId() {
		return this.guildId;
	}

	@JsonIgnore
	public Snowflake getUserId() {
		return this.userId;
	}

	@JsonIgnore
	public int getNumber() {
		return this.number;
	}

}
