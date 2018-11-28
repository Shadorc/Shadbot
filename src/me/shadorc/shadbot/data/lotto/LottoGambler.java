package me.shadorc.shadbot.data.lotto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

public class LottoGambler {

	@JsonProperty("guild_id")
	private final Long guildId;
	@JsonProperty("user_id")
	private final Long userId;
	@JsonProperty("number")
	private final int number;

	public LottoGambler(Snowflake guildId, Snowflake userId, int number) {
		this.guildId = guildId.asLong();
		this.userId = userId.asLong();
		this.number = number;
	}

	public LottoGambler() {
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
		return String.format("LottoGambler [guildId=%s, userId=%s, number=%s]", this.guildId, this.userId, this.number);
	}

}
