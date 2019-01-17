package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class LotteryGambler {

	@JsonProperty("guild_id")
	private final long guildId;
	@JsonProperty("user_id")
	private final long userId;
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

	public Snowflake getGuildId() {
		return Snowflake.of(this.guildId);
	}

	public Snowflake getUserId() {
		return Snowflake.of(this.userId);
	}

	public int getNumber() {
		return this.number;
	}

}
