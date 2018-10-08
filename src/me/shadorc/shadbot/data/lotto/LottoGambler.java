package me.shadorc.shadbot.data.lotto;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

public class LottoGambler {

	@JsonProperty("guild_id")
	private Long guildId;
	@JsonProperty("user_id")
	private Long userId;
	@JsonProperty("number")
	private int number;

	public LottoGambler() {
		this.guildId = null;
		this.userId = null;
		this.number = 0;
	}

	public LottoGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId.asLong();
		this.userId = userId.asLong();
		this.number = number;
	}

	public Snowflake getGuildId() {
		return Snowflake.of(this.guildId);
	}

	public Snowflake getUserId() {
		return Snowflake.of(this.userId);
	}

	public int getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		return String.format("LottoGambler [guildId=%s, userId=%s, number=%s]", this.guildId, this.userId, this.number);
	}

}
