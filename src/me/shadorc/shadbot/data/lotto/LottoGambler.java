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

	public LottoGambler() {
		this.guildId = null;
		this.userId = null;
		this.number = 0;
	}

	public LottoGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId;
		this.userId = userId;
		this.number = number;
	}

	public Snowflake getGuildId() {
		return this.guildId;
	}

	public Snowflake getUserId() {
		return this.userId;
	}

	public int getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		return String.format("LottoGambler [guildId=%s, userId=%s, number=%s]", this.guildId, this.userId, this.number);
	}

}
