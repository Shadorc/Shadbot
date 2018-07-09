package me.shadorc.shadbot.data.lotto;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

public class LottoGambler {

	@JsonProperty("guild_id")
	private Snowflake guildId;
	@JsonProperty("user_id")
	private Snowflake userId;
	@JsonProperty("number")
	private int number;

	public LottoGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId;
		this.userId = userId;
		this.number = number;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getUserId() {
		return userId;
	}

	public int getNumber() {
		return number;
	}

}
